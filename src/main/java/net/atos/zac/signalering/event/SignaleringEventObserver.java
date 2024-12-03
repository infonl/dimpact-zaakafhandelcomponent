/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.signalering.event;

import static net.atos.zac.util.UriUtilKt.uuidFromURI;

import java.net.URI;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.flowable.task.api.TaskInfo;

import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.BetrokkeneType;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.RolListParameters;
import net.atos.client.zgw.zrc.model.RolMedewerker;
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.zac.event.AbstractEventObserver;
import net.atos.zac.flowable.task.FlowableTaskService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.User;
import net.atos.zac.signalering.SignaleringService;
import net.atos.zac.signalering.model.Signalering;
import net.atos.zac.signalering.model.SignaleringInstellingen;

/**
 * This bean listens for SignaleringEvents and handles them.
 */
@Named
@ApplicationScoped
public class SignaleringEventObserver extends AbstractEventObserver<SignaleringEvent<?>> {

    private static final Logger LOG = Logger.getLogger(SignaleringEventObserver.class.getName());

    private ZtcClientService ztcClientService;
    private ZrcClientService zrcClientService;
    private FlowableTaskService flowableTaskService;
    private IdentityService identityService;
    private SignaleringService signaleringService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public SignaleringEventObserver() {
    }

    @Inject
    public SignaleringEventObserver(
            final ZtcClientService ztcClientService,
            final ZrcClientService zrcClientService,
            final FlowableTaskService flowableTaskService,
            final IdentityService identityService,
            final SignaleringService signaleringService
    ) {
        this.ztcClientService = ztcClientService;
        this.zrcClientService = zrcClientService;
        this.flowableTaskService = flowableTaskService;
        this.identityService = identityService;
        this.signaleringService = signaleringService;
    }

    @Override
    public void onFire(final @ObservesAsync SignaleringEvent<?> event) {
        try {
            LOG.fine(() -> String.format("Signalering event ontvangen: %s", event));
            event.delay();

            final Signalering signalering = buildSignalering(event);
            if (signalering == null) {
                LOG.fine(() -> String.format("No signal generated for received event: %s", event));
                return;
            }
            if (!signaleringService.isNecessary(signalering, event.getActor())) {
                LOG.fine(() -> String.format("Unnecessary signalering: %s for actor %s", signalering, event.getActor()));
                return;
            }

            final SignaleringInstellingen subscriptions = signaleringService.readInstellingen(signalering);
            LOG.fine(() -> String.format("Subscription settings: %s for signalering: %s", subscriptions, signalering));
            if (subscriptions.isDashboard()) {
                signaleringService.storeSignalering(signalering);
            }
            if (subscriptions.isMail()) {
                signaleringService.sendSignalering(signalering);
            }
        } catch (final Throwable ex) {
            LOG.log(Level.SEVERE, "asynchronous guard", ex);
        }
    }

    private Signalering getInstance(final SignaleringEvent<?> event) {
        return signaleringService.signaleringInstance(event.getObjectType());
    }

    private Signalering getSignaleringVoorRol(final SignaleringEvent<?> event, final Zaak subject, final Rol<?> rol) {
        final Signalering signalering = getInstance(event);
        signalering.setSubject(subject);
        return addTarget(signalering, rol);
    }

    private Signalering getSignaleringVoorMedewerker(
            final SignaleringEvent<?> event,
            final Zaak subject,
            final RolMedewerker rol
    ) {
        return getSignaleringVoorRol(event, subject, rol);
    }

    private Signalering getSignaleringVoorGroup(
            final SignaleringEvent<?> event,
            final Zaak subject,
            final RolOrganisatorischeEenheid rol
    ) {
        if (getRolBehandelaarMedewerker(subject).isEmpty()) {
            return getSignaleringVoorRol(event, subject, rol);
        }
        return null;
    }

    private @Nullable Signalering getSignaleringVoorBehandelaar(
            final SignaleringEvent<?> event,
            final Zaak subject,
            final ZaakInformatieobject detail
    ) {
        final Optional<Rol<?>> behandelaar = getRolBehandelaarMedewerker(subject);
        if (behandelaar.isPresent()) {
            final Signalering signalering = getSignaleringVoorRol(event, subject, behandelaar.get());
            if (signalering != null) {
                signalering.setDetailFromZaakInformatieobject(detail);
            }
            return signalering;
        }
        return null;
    }

    private Signalering getSignaleringVoorBehandelaar(final SignaleringEvent<?> event, final TaskInfo subject) {
        if (subject.getAssignee() != null) {
            final Signalering signalering = getInstance(event);
            signalering.setSubject(subject);
            return addTarget(signalering, subject);
        }
        return null;
    }

    // On creation of a human task it's owner is assumed to be the actor who created it.
    private SignaleringEvent<?> fixActor(final SignaleringEvent<?> event, final TaskInfo subject) {
        if (event.getActor() == null) {
            final String owner = subject.getOwner();
            final User actor = owner != null ? identityService.readUser(owner) : null;
            final SignaleringEvent<?> fixed = SignaleringEventUtil.event(event.getObjectType(), subject, actor);
            if (actor != null) {
                LOG.fine(() -> String.format("Signalering event fixed: %s", fixed));
            }
            return fixed;
        }
        return event;
    }

    private Signalering buildSignalering(final SignaleringEvent<?> event) {
        switch (event.getObjectType()) {
            case ZAAK_DOCUMENT_TOEGEVOEGD -> {
                final Zaak subject = zrcClientService.readZaak((URI) event.getObjectId().resource());
                final ZaakInformatieobject detail = zrcClientService.readZaakinformatieobject(
                        uuidFromURI((URI) event.getObjectId().detail()));
                return getSignaleringVoorBehandelaar(event, subject, detail);
            }
            case ZAAK_OP_NAAM -> {
                final Rol<?> rol = zrcClientService.readRol((URI) event.getObjectId().resource());
                if (OmschrijvingGeneriekEnum.valueOf(rol.getOmschrijvingGeneriek().toUpperCase()) ==
                    OmschrijvingGeneriekEnum.BEHANDELAAR) {
                    final Zaak subject = zrcClientService.readZaak(rol.getZaak());
                    switch (rol.getBetrokkeneType()) {
                        case MEDEWERKER -> {
                            return getSignaleringVoorMedewerker(event, subject, (RolMedewerker) rol);
                        }
                        case ORGANISATORISCHE_EENHEID -> {
                            return getSignaleringVoorGroup(event, subject, (RolOrganisatorischeEenheid) rol);
                        }
                        default -> LOG.warning(String.format("unexpected BetrokkeneType %s", rol.getBetrokkeneType()));
                    }
                }
            }
            case TAAK_OP_NAAM -> {
                final TaskInfo subject = flowableTaskService.readOpenTask((String) event.getObjectId().resource());
                return getSignaleringVoorBehandelaar(fixActor(event, subject), subject);
            }
            case ZAAK_VERLOPEND, TAAK_VERLOPEN ->
                // These are NOT event driven and should not show up here
                LOG.warning(String.format("ignored SignaleringType %s", event.getObjectType()));
        }
        return null;
    }

    private RolType getRoltypeBehandelaar(final Zaak zaak) {
        return ztcClientService.readRoltype(zaak.getZaaktype(), OmschrijvingGeneriekEnum.BEHANDELAAR);
    }

    private Optional<Rol<?>> getRolBehandelaarMedewerker(final Zaak zaak) {
        return zrcClientService.listRollen(
                new RolListParameters(
                        zaak.getUrl(),
                        getRoltypeBehandelaar(zaak).getUrl(),
                        BetrokkeneType.MEDEWERKER)
        ).getSingleResult();
    }

    private @Nullable Signalering addTarget(final Signalering signalering, final Rol<?> rol) {
        switch (rol.getBetrokkeneType()) {
            case MEDEWERKER -> {
                final RolMedewerker rolMedewerker = (RolMedewerker) rol;
                return addTargetUser(signalering, rolMedewerker.getBetrokkeneIdentificatie().getIdentificatie());
            }
            case ORGANISATORISCHE_EENHEID -> {
                final RolOrganisatorischeEenheid rolGroep = (RolOrganisatorischeEenheid) rol;
                return addTargetGroup(signalering, rolGroep.getBetrokkeneIdentificatie().getIdentificatie());
            }
            default -> {
                LOG.log(Level.WARNING, "Unknown BetrokkeneType '{0}'", rol.getBetrokkeneType());
                return null;
            }
        }
    }

    private Signalering addTarget(final Signalering signalering, final TaskInfo taskInfo) {
        return addTargetUser(signalering, taskInfo.getAssignee());
    }

    private Signalering addTargetUser(final Signalering signalering, final String userId) {
        signalering.setTarget(identityService.readUser(userId));
        return signalering;
    }

    private Signalering addTargetGroup(final Signalering signalering, final String groupId) {
        signalering.setTarget(identityService.readGroup(groupId));
        return signalering;
    }
}
