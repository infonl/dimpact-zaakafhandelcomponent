/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy;

import static net.atos.client.zgw.drc.model.generated.StatusEnum.DEFINITIEF;
import static net.atos.client.zgw.util.UriUtilsKt.extractUuid;
import static net.atos.client.zgw.zrc.util.StatusTypeUtil.isHeropend;
import static net.atos.client.zgw.zrc.util.StatusTypeUtil.isIntake;
import static net.atos.zac.enkelvoudiginformatieobject.util.EnkelvoudigInformatieObjectCheckersKt.isSigned;
import static net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving;
import static net.atos.zac.flowable.util.TaskUtil.isOpen;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.flowable.task.api.TaskInfo;

import net.atos.client.opa.model.RuleQuery;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;
import net.atos.zac.policy.exception.PolicyException;
import net.atos.zac.policy.input.DocumentData;
import net.atos.zac.policy.input.DocumentInput;
import net.atos.zac.policy.input.TaakData;
import net.atos.zac.policy.input.TaakInput;
import net.atos.zac.policy.input.UserInput;
import net.atos.zac.policy.input.ZaakData;
import net.atos.zac.policy.input.ZaakInput;
import net.atos.zac.policy.output.DocumentRechten;
import net.atos.zac.policy.output.OverigeRechten;
import net.atos.zac.policy.output.TaakRechten;
import net.atos.zac.policy.output.WerklijstRechten;
import net.atos.zac.policy.output.ZaakRechten;
import net.atos.zac.shared.exception.FoutmeldingException;
import net.atos.zac.zoeken.model.DocumentIndicatie;
import net.atos.zac.zoeken.model.ZaakIndicatie;
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject;
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject;
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject;

@ApplicationScoped
public class PolicyService {
    private Instance<LoggedInUser> loggedInUserInstance;
    private OPAEvaluationClient evaluationClient;
    private ZtcClientService ztcClientService;
    private EnkelvoudigInformatieObjectLockService lockService;
    private ZrcClientService zrcClientService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public PolicyService() {
    }

    @Inject
    public PolicyService(
            final Instance<LoggedInUser> loggedInUserInstance,
            final @RestClient OPAEvaluationClient evaluationClient,
            final ZtcClientService ztcClientService,
            final EnkelvoudigInformatieObjectLockService lockService,
            final ZrcClientService zrcClientService
    ) {
        this.loggedInUserInstance = loggedInUserInstance;
        this.evaluationClient = evaluationClient;
        this.ztcClientService = ztcClientService;
        this.lockService = lockService;
        this.zrcClientService = zrcClientService;
    }

    public OverigeRechten readOverigeRechten() {
        return evaluationClient.readOverigeRechten(
                new RuleQuery<>(new UserInput(loggedInUserInstance.get()))
        ).getResult();
    }

    public ZaakRechten readZaakRechten(final Zaak zaak) {
        return readZaakRechten(zaak, ztcClientService.readZaaktype(zaak.getZaaktype()));
    }

    public ZaakRechten readZaakRechten(final Zaak zaak, final ZaakType zaaktype) {
        final ZaakData zaakData = new ZaakData();
        zaakData.open = zaak.isOpen();
        zaakData.zaaktype = zaaktype.getOmschrijving();
        zaakData.opgeschort = zaak.isOpgeschort();
        zaakData.verlengd = zaak.isVerlengd();
        zaakData.besloten = CollectionUtils.isNotEmpty(zaaktype.getBesluittypen());
        StatusType statusType = null;
        if (zaak.getStatus() != null) {
            var status = zrcClientService.readStatus(zaak.getStatus());
            statusType = ztcClientService.readStatustype(status.getStatustype());
        }
        zaakData.intake = isIntake(statusType);
        zaakData.heropend = isHeropend(statusType);
        return evaluationClient.readZaakRechten(new RuleQuery<>(
                new ZaakInput(loggedInUserInstance.get(), zaakData))
        ).getResult();
    }

    public ZaakRechten readZaakRechten(final ZaakZoekObject zaakZoekObject) {
        final ZaakData zaakData = new ZaakData();
        zaakData.open = !zaakZoekObject.isAfgehandeld();
        zaakData.zaaktype = zaakZoekObject.getZaaktypeOmschrijving();
        zaakData.opgeschort = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.OPSCHORTING);
        zaakData.verlengd = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.VERLENGD);
        zaakData.heropend = zaakZoekObject.getZaakIndicaties().contains(ZaakIndicatie.HEROPEND);

        return evaluationClient.readZaakRechten(new RuleQuery<>(new ZaakInput(loggedInUserInstance.get(), zaakData)))
                .getResult();
    }

    public DocumentRechten readDocumentRechten(final EnkelvoudigInformatieObject enkelvoudigInformatieobject) {
        return readDocumentRechten(enkelvoudigInformatieobject, null);
    }

    public DocumentRechten readDocumentRechten(final EnkelvoudigInformatieObject enkelvoudigInformatieobject, final Zaak zaak) {
        return readDocumentRechten(
                enkelvoudigInformatieobject,
                lockService.findLock(extractUuid(enkelvoudigInformatieobject.getUrl())),
                zaak
        );
    }

    public DocumentRechten readDocumentRechten(
            final EnkelvoudigInformatieObject enkelvoudigInformatieobject,
            final EnkelvoudigInformatieObjectLock lock,
            final Zaak zaak
    ) {
        final DocumentData documentData = new DocumentData();
        documentData.definitief = enkelvoudigInformatieobject.getStatus() == DEFINITIEF;
        documentData.vergrendeld = enkelvoudigInformatieobject.getLocked();
        documentData.vergrendeldDoor = lock != null ? lock.getUserId() : null;
        documentData.ondertekend = isSigned(enkelvoudigInformatieobject);
        if (zaak != null) {
            documentData.zaakOpen = zaak.isOpen();
            documentData.zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype()).getOmschrijving();
        }
        return evaluationClient.readDocumentRechten(
                new RuleQuery<>(new DocumentInput(loggedInUserInstance.get(), documentData))).getResult();
    }

    public DocumentRechten readDocumentRechten(final DocumentZoekObject enkelvoudigInformatieobject) {
        final DocumentData documentData = new DocumentData();
        documentData.definitief = DEFINITIEF.equals(enkelvoudigInformatieobject.getStatus());
        documentData.vergrendeld = enkelvoudigInformatieobject.isIndicatie(DocumentIndicatie.VERGRENDELD);
        documentData.vergrendeldDoor = enkelvoudigInformatieobject.getVergrendeldDoorGebruikersnaam();
        documentData.zaakOpen = !enkelvoudigInformatieobject.isZaakAfgehandeld();
        documentData.zaaktype = enkelvoudigInformatieobject.getZaaktypeOmschrijving();
        documentData.ondertekend = enkelvoudigInformatieobject.getOndertekeningDatum() != null;
        return evaluationClient.readDocumentRechten(
                new RuleQuery<>(new DocumentInput(loggedInUserInstance.get(), documentData))).getResult();
    }

    public TaakRechten readTaakRechten(final TaskInfo taskInfo) {
        return readTaakRechten(taskInfo, readZaaktypeOmschrijving(taskInfo));
    }

    public TaakRechten readTaakRechten(final TaskInfo taskInfo, final String zaaktypeOmschrijving) {
        final TaakData taakData = new TaakData();
        taakData.open = isOpen(taskInfo);
        taakData.zaaktype = zaaktypeOmschrijving;
        return evaluationClient.readTaakRechten(new RuleQuery<>(new TaakInput(loggedInUserInstance.get(), taakData)))
                .getResult();
    }

    public TaakRechten readTaakRechten(final TaakZoekObject taakZoekObject) {
        final TaakData taakData = new TaakData();
        taakData.zaaktype = taakZoekObject.getZaaktypeOmschrijving();
        return evaluationClient.readTaakRechten(
                new RuleQuery<>(new TaakInput(loggedInUserInstance.get(), taakData))
        ).getResult();
    }

    public WerklijstRechten readWerklijstRechten() {
        return evaluationClient.readWerklijstRechten(
                new RuleQuery<>(new UserInput(loggedInUserInstance.get()))
        ).getResult();
    }

    public void checkZaakAfsluitbaar(final Zaak zaak) {
        if (zrcClientService.heeftOpenDeelzaken(zaak)) {
            throw new FoutmeldingException("Deze hoofdzaak heeft open deelzaken en kan niet afgesloten worden.");
        }
        if (lockService.hasLockedInformatieobjecten(zaak)) {
            throw new FoutmeldingException("Deze zaak heeft vergrendelde documenten en kan niet afgesloten worden.");
        }
    }

    public static void assertPolicy(final boolean policy) {
        if (!policy) {
            throw new PolicyException();
        }
    }
}
