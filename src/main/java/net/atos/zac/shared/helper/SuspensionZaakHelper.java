package net.atos.zac.shared.helper;

import static net.atos.zac.policy.PolicyService.assertPolicy;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.generated.Opschorting;
import net.atos.zac.flowable.ZaakVariabelenService;
import net.atos.zac.policy.PolicyService;

public class SuspensionZaakHelper {
    private static final String SUSPENSION = "Opschorting";
    private static final String RESUMING = "Hervatting";

    private PolicyService policyService;
    private ZrcClientService zrcClientService;
    private ZaakVariabelenService zaakVariabelenService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public SuspensionZaakHelper() {
    }

    @Inject
    SuspensionZaakHelper(
            final PolicyService policyService,
            final ZrcClientService zrcClientService,
            final ZaakVariabelenService zaakVariabelenService
    ) {
        this.policyService = policyService;
        this.zrcClientService = zrcClientService;
        this.zaakVariabelenService = zaakVariabelenService;
    }

    public Zaak suspendZaak(Zaak zaak, final long numberOfDays, final String suspensionReason) {
        assertPolicy(policyService.readZaakRechten(zaak).opschorten());
        assertPolicy(StringUtils.isEmpty(zaak.getOpschorting().getReden()));
        final UUID zaakUUID = zaak.getUuid();
        final String toelichting = String.format("%s: %s", SUSPENSION, suspensionReason);
        LocalDate einddatumGepland = null;
        if (zaak.getEinddatumGepland() != null) {
            einddatumGepland = zaak.getEinddatumGepland().plusDays(numberOfDays);
        }
        final LocalDate uiterlijkeEinddatumAfdoening = zaak.getUiterlijkeEinddatumAfdoening().plusDays(numberOfDays);
        final var patchZaak = addSuspensionToZaakPatch(
                createZaakPatch(einddatumGepland, uiterlijkeEinddatumAfdoening),
                suspensionReason,
                true
        );

        final Zaak updatedZaak = zrcClientService.patchZaak(zaakUUID, patchZaak, toelichting);
        zaakVariabelenService.setDatumtijdOpgeschort(zaakUUID, ZonedDateTime.now());
        zaakVariabelenService.setVerwachteDagenOpgeschort(zaakUUID, Math.toIntExact(numberOfDays));
        return updatedZaak;
    }

    public Zaak resumeZaak(final Zaak zaak, final String resumeReason) {
        assertPolicy(policyService.readZaakRechten(zaak).hervatten());
        assertPolicy(zaak.isOpgeschort());
        final UUID zaakUUID = zaak.getUuid();
        final ZonedDateTime datumOpgeschort = zaakVariabelenService.findDatumtijdOpgeschort(zaak.getUuid()).orElseGet(ZonedDateTime::now);
        final int verwachteDagenOpgeschort = zaakVariabelenService.findVerwachteDagenOpgeschort(zaak.getUuid()).orElse(0);
        final long dagenVerschil = ChronoUnit.DAYS.between(datumOpgeschort, ZonedDateTime.now());
        final long offset = dagenVerschil - verwachteDagenOpgeschort;
        LocalDate einddatumGepland = null;
        if (zaak.getEinddatumGepland() != null) {
            einddatumGepland = zaak.getEinddatumGepland().plusDays(offset);
        }
        final LocalDate uiterlijkeEinddatumAfdoening = zaak.getUiterlijkeEinddatumAfdoening().plusDays(offset);

        final String toelichting = String.format("%s: %s", RESUMING, resumeReason);
        final var patchZaak = addSuspensionToZaakPatch(
                createZaakPatch(einddatumGepland, uiterlijkeEinddatumAfdoening),
                resumeReason,
                false
        );

        final Zaak updatedZaak = zrcClientService.patchZaak(zaakUUID, patchZaak, toelichting);
        zaakVariabelenService.removeDatumtijdOpgeschort(zaakUUID);
        zaakVariabelenService.removeVerwachteDagenOpgeschort(zaakUUID);
        return updatedZaak;
    }

    public Zaak extendZaakFatalDate(final Zaak zaak, final long numberOfDays, final String description) {
        var zaakRechten = policyService.readZaakRechten(zaak);
        assertPolicy(zaakRechten.wijzigen() && zaakRechten.verlengenDoorlooptijd());

        final UUID zaakUUID = zaak.getUuid();
        LocalDate endDatePlanned = null;
        if (zaak.getEinddatumGepland() != null) {
            endDatePlanned = zaak.getEinddatumGepland().plusDays(numberOfDays);
        }
        final LocalDate finalCompletionDate = zaak.getUiterlijkeEinddatumAfdoening().plusDays(numberOfDays);

        return zrcClientService.patchZaak(zaakUUID, createZaakPatch(endDatePlanned, finalCompletionDate), description);
    }

    private Zaak createZaakPatch(
            final LocalDate endDatePlanned,
            final LocalDate finalCompletionDate
    ) {
        final Zaak zaak = new Zaak();
        if (endDatePlanned != null) {
            zaak.setEinddatumGepland(endDatePlanned);
        }
        zaak.setUiterlijkeEinddatumAfdoening(finalCompletionDate);
        return zaak;
    }

    private Zaak addSuspensionToZaakPatch(
            final Zaak zaak,
            final String reason,
            final boolean isOpschorting
    ) {
        final Opschorting opschorting = new Opschorting();
        opschorting.setReden(reason);
        opschorting.setIndicatie(isOpschorting);
        zaak.setOpschorting(opschorting);
        return zaak;
    }

}
