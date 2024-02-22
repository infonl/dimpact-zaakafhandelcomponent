/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared;

import static net.atos.client.zgw.shared.util.DateTimeUtil.convertToDateTime;

import java.net.URI;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectData;
import net.atos.client.zgw.drc.model.generated.Gebruiksrechten;
import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.BetrokkeneType;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.RolListParameters;
import net.atos.client.zgw.zrc.model.RolMedewerker;
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.zrc.model.generated.Resultaat;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.BrondatumArchiefprocedure;
import net.atos.client.zgw.ztc.model.generated.ResultaatType;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;


/**
 * Careful!
 * <p>
 * Never call methods with caching annotations from within the service (or it will not work).
 * Do not introduce caches with keys other than URI and UUID.
 * Use Optional for caches that need to hold nulls (Infinispan does not cache nulls).
 */
@ApplicationScoped
public class ZGWApiService {

    private static final Logger LOG = Logger.getLogger(ZGWApiService.class.getName());

    // Page numbering in ZGW Api's starts with 1
    public static final int FIRST_PAGE_NUMBER_ZGW_APIS = 1;

    @Inject
    private ZTCClientService ztcClientService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private DRCClientService drcClientService;

    /**
     * Create {@link Zaak} and calculate Doorlooptijden.
     *
     * @param zaak {@link Zaak}
     * @return Created {@link Zaak}
     */
    public Zaak createZaak(final Zaak zaak) {
        calculateDoorlooptijden(zaak);
        return zrcClientService.createZaak(zaak);
    }

    /**
     * Create {@link Status} for a given {@link Zaak} based on {@link StatusType}.omschrijving
     * and with {@link Status}.toelichting.
     *
     * @param zaak                   {@link Zaak}
     * @param statusTypeOmschrijving Omschrijving of the {@link StatusType} of the required
     * {@link Status}.
     * @param statusToelichting      Toelichting for thew {@link Status}.
     * @return Created {@link Status}.
     */
    public Status createStatusForZaak(final Zaak zaak, final String statusTypeOmschrijving,
            final String statusToelichting) {
        final StatusType statustype =
                readStatustype(ztcClientService.readStatustypen(zaak.getZaaktype()),
                                                     statusTypeOmschrijving, zaak.getZaaktype());
        return createStatusForZaak(zaak.getUrl(), statustype.getUrl(), statusToelichting);
    }

    /**
     * Create {@link Resultaat} for a given {@link Zaak} based on {@link ResultaatType}
     * .omschrijving and with {@link Resultaat}.toelichting.
     *
     * @param zaak                      {@link Zaak}
     * @param resultaattypeOmschrijving Omschrijving of the {@link ResultaatType} of the required
     * {@link Resultaat}.
     * @param resultaatToelichting      Toelichting for thew {@link Resultaat}.
     * @return Created {@link Resultaat}.
     */
    public Resultaat createResultaatForZaak(final Zaak zaak, final String resultaattypeOmschrijving,
            final String resultaatToelichting) {
        final List<ResultaatType> resultaattypen =
                ztcClientService.readResultaattypen(zaak.getZaaktype());
        final ResultaatType resultaattype = filterResultaattype(resultaattypen,
                                                                resultaattypeOmschrijving,
                                                                zaak.getZaaktype());
        return createResultaat(zaak.getUrl(), resultaattype.getUrl(), resultaatToelichting);
    }

    /**
     * Create {@link Resultaat} for a given {@link Zaak} based on {@link ResultaatType}.UUID and
     * with {@link Resultaat}.toelichting.
     *
     * @param zaak                 {@link Zaak}
     * @param resultaattypeUUID    UUID of the {@link ResultaatType} of the required
     * {@link Resultaat}.
     * @param resultaatToelichting Toelichting for thew {@link Resultaat}.
     * @return Created {@link Resultaat}.
     */
    public Resultaat createResultaatForZaak(final Zaak zaak, final UUID resultaattypeUUID,
            final String resultaatToelichting) {
        final ResultaatType resultaattype = ztcClientService.readResultaattype(resultaattypeUUID);
        return createResultaat(zaak.getUrl(), resultaattype.getUrl(), resultaatToelichting);
    }

    /**
     * Update {@link Resultaat} for a given {@link Zaak} based on {@link ResultaatType}.UUID and
     * with {@link Resultaat}
     * .toelichting.
     *
     * @param zaak              {@link Zaak}
     * @param resultaatTypeUuid Containing the UUID of the {@link ResultaatType} of the required
     * {@link Resultaat}.
     * @param reden             Reason of setting the {@link ResultaatType}
     * @return Created {@link Resultaat}.
     */
    public Resultaat updateResultaatForZaak(final Zaak zaak, final UUID resultaatTypeUuid, final String reden) {
        final Resultaat resultaat = zrcClientService.readResultaat(zaak.getResultaat());
        zrcClientService.deleteResultaat(resultaat.getUuid());
        return createResultaatForZaak(zaak, resultaatTypeUuid, reden);
    }

    /**
     * End {@link Zaak}.
     * Creating a new Eind {@link Status} for the {@link Zaak}.
     * And calculating the archiverings parameters
     *
     * @param zaak                  {@link Zaak}
     * @param eindstatusToelichting Toelichting for thew Eind {@link Status}.
     */
    public void endZaak(final Zaak zaak, final String eindstatusToelichting) {
        closeZaak(zaak, eindstatusToelichting);
        berekenArchiveringsparameters(zaak.getUuid());
    }

    /**
     * End {@link Zaak}.
     * Creating a new Eind {@link Status} for the {@link Zaak}.
     * And calculating the archiverings parameters
     *
     * @param zaakUUID              UUID of the {@link Zaak}
     * @param eindstatusToelichting Toelichting for thew Eind {@link Status}.
     */
    public void endZaak(final UUID zaakUUID, final String eindstatusToelichting) {
        final Zaak zaak = zrcClientService.readZaak(zaakUUID);
        endZaak(zaak, eindstatusToelichting);
    }

    /**
     * Close {@link Zaak}.
     * Creating a new Eind {@link Status} for the {@link Zaak}.
     *
     * @param zaak                  {@link Zaak} to be closed
     * @param eindstatusToelichting Toelichting for thew Eind {@link Status}.
     */
    public void closeZaak(final Zaak zaak, final String eindstatusToelichting) {
        final StatusType eindStatustype =
                readStatustypeEind(ztcClientService.readStatustypen(zaak.getZaaktype()),
                                                             zaak.getZaaktype());
        createStatusForZaak(zaak.getUrl(), eindStatustype.getUrl(), eindstatusToelichting);
    }

    /**
     * Create {@link EnkelvoudigInformatieObject} and {@link ZaakInformatieobject} for {@link Zaak}.
     *
     * @param zaak                                   {@link Zaak}.
     * @param enkelvoudigInformatieObjectData                       {@link EnkelvoudigInformatieObject} to be
     *                                                                                  created.
     * @param titel                                  Titel of the new {@link ZaakInformatieobject}.
     * @param beschrijving                           Beschrijving of the new {@link ZaakInformatieobject}.
     * @param omschrijvingVoorwaardenGebruiksrechten Used to create the {@link Gebruiksrechten}
     *                                              for the to be created {@link EnkelvoudigInformatieObject}
     * @return Created {@link ZaakInformatieobject}.
     */
    public ZaakInformatieobject createZaakInformatieobjectForZaak(
            final Zaak zaak,
            final EnkelvoudigInformatieObjectData enkelvoudigInformatieObjectData,
            final String titel,
            final String beschrijving,
            final String omschrijvingVoorwaardenGebruiksrechten
    ) {
        final EnkelvoudigInformatieObject newInformatieObjectData =
                drcClientService.createEnkelvoudigInformatieobject(enkelvoudigInformatieObjectData);
        final Gebruiksrechten gebruiksrechten = new Gebruiksrechten();
        gebruiksrechten.setInformatieobject(newInformatieObjectData.getUrl());
        gebruiksrechten.setStartdatum(convertToDateTime(newInformatieObjectData.getCreatiedatum()).toOffsetDateTime());
        gebruiksrechten.setOmschrijvingVoorwaarden(omschrijvingVoorwaardenGebruiksrechten);
        drcClientService.createGebruiksrechten(gebruiksrechten);

        final ZaakInformatieobject zaakInformatieObject = new ZaakInformatieobject();
        zaakInformatieObject.setZaak(zaak.getUrl());
        zaakInformatieObject.setInformatieobject(newInformatieObjectData.getUrl());
        zaakInformatieObject.setTitel(titel);
        zaakInformatieObject.setBeschrijving(beschrijving);
        return zrcClientService.createZaakInformatieobject(zaakInformatieObject, StringUtils.EMPTY);
    }

    /**
     * Delete {@link ZaakInformatieobject} which relates {@link EnkelvoudigInformatieObject} and
     * {@link Zaak} with zaakUUID.
     * When the {@link EnkelvoudigInformatieObject} has no other related
     * {@link ZaakInformatieobject}s then it is also deleted.
     *
     * @param enkelvoudigInformatieobject {@link EnkelvoudigInformatieObject}
     * @param zaakUUID                    UUID of a {@link Zaak}
     * @param toelichting                 Explanation why the {@link EnkelvoudigInformatieObject}
     *                                   is to be removed.
     */
    public void removeEnkelvoudigInformatieObjectFromZaak(final EnkelvoudigInformatieObject enkelvoudigInformatieobject,
            final UUID zaakUUID,
            final String toelichting) {
        final List<ZaakInformatieobject> zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(
                enkelvoudigInformatieobject);
        // Delete the relationship of the EnkelvoudigInformatieobject with the zaak.
        zaakInformatieobjecten.stream()
                .filter(zaakInformatieobject -> zaakInformatieobject.getZaakUUID().equals(zaakUUID))
                .forEach(zaakInformatieobject -> zrcClientService.deleteZaakInformatieobject(zaakInformatieobject.getUuid(),
                                                                                             toelichting,
                                                                                             "Verwijderd"));

        // If the EnkelvoudigInformatieobject has no relationship(s) with other zaken it can be deleted.
        if (zaakInformatieobjecten.stream()
                .allMatch(zaakInformatieobject -> zaakInformatieobject.getZaakUUID().equals(zaakUUID))) {
            drcClientService.deleteEnkelvoudigInformatieobject(URIUtil.parseUUIDFromResourceURI(enkelvoudigInformatieobject.getUrl()));
        }
    }

    /**
     * Find {@link RolOrganisatorischeEenheid} for {@link Zaak} with behandelaar {@link RolType.OmschrijvingGeneriekEnum}.
     *
     * @param zaak {@link Zaak}
     * @return {@link RolOrganisatorischeEenheid} or 'null'.
     */
    public Optional<RolOrganisatorischeEenheid> findGroepForZaak(final Zaak zaak) {
        return findRolForZaak(zaak, RolType.OmschrijvingGeneriekEnum.BEHANDELAAR, BetrokkeneType.ORGANISATORISCHE_EENHEID)
                .map(RolOrganisatorischeEenheid.class::cast);
    }

    /**
     * Find {@link RolMedewerker} for {@link Zaak} with behandelaar {@link RolType.OmschrijvingGeneriekEnum}.
     *
     * @param zaak {@link Zaak}
     * @return {@link RolMedewerker} or 'null'.
     */
    public Optional<RolMedewerker> findBehandelaarForZaak(final Zaak zaak) {
        return findRolForZaak(zaak, RolType.OmschrijvingGeneriekEnum.BEHANDELAAR, BetrokkeneType.MEDEWERKER)
                .map(RolMedewerker.class::cast);
    }

    public Optional<Rol<?>> findInitiatorForZaak(final Zaak zaak) {
        return findRolForZaak(zaak, RolType.OmschrijvingGeneriekEnum.INITIATOR);
    }

    private Optional<Rol<?>> findRolForZaak(final Zaak zaak, final RolType.OmschrijvingGeneriekEnum omschrijvingGeneriekEnum) {
        return ztcClientService.findRoltype(zaak.getZaaktype(), omschrijvingGeneriekEnum)
                .flatMap(roltype -> zrcClientService.listRollen(new RolListParameters(zaak.getUrl(), roltype.getUrl()))
                        .getSingleResult());
    }

    private Optional<Rol<?>> findRolForZaak(final Zaak zaak, final RolType.OmschrijvingGeneriekEnum omschrijvingGeneriekEnum,
            final BetrokkeneType betrokkeneType) {
        return ztcClientService.findRoltype(zaak.getZaaktype(), omschrijvingGeneriekEnum).
                flatMap(roltype -> zrcClientService.listRollen(
                                new RolListParameters(zaak.getUrl(), roltype.getUrl(), betrokkeneType))
                        .getSingleResult());
    }

    private Status createStatusForZaak(final URI zaakURI, final URI statustypeURI, final String toelichting) {
        final Status status = new Status(zaakURI, statustypeURI, ZonedDateTime.now());
        status.setStatustoelichting(toelichting);
        return zrcClientService.createStatus(status);
    }

    private void calculateDoorlooptijden(final Zaak zaak) {
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());

        if (zaaktype.getServicenorm() != null) {
            zaak.setEinddatumGepland(zaak.getStartdatum().plus(Period.parse(zaaktype.getServicenorm())));
        }

        zaak.setUiterlijkeEinddatumAfdoening(zaak.getStartdatum().plus(Period.parse(zaaktype.getDoorlooptijd())));
    }

    private Resultaat createResultaat(
            final URI zaakURI,
            final URI resultaattypeURI,
            final String resultaatToelichting
    ) {
        final Resultaat resultaat = new Resultaat();
        resultaat.setZaak(zaakURI);
        resultaat.setResultaattype(resultaattypeURI);
        resultaat.setToelichting(resultaatToelichting);
        return zrcClientService.createResultaat(resultaat);
    }

    private ResultaatType filterResultaattype(List<ResultaatType> resultaattypes,
            final String omschrijving,
            final URI zaaktypeURI) {
        return resultaattypes.stream()
                .filter(resultaattype -> StringUtils.equals(resultaattype.getOmschrijving(), omschrijving))
                .findAny()
                .orElseThrow(
                        () -> new RuntimeException(
                                String.format("Zaaktype '%s': Resultaattype with omschrijving '%s' not found",
                                              zaaktypeURI, omschrijving)));
    }

    private void berekenArchiveringsparameters(final UUID zaakUUID) {
        final Zaak zaak = zrcClientService.readZaak(
                zaakUUID); // refetch to get the einddatum (the archiefnominatie has also been set)
        final ResultaatType resultaattype = ztcClientService.readResultaattype(
                zrcClientService.readResultaat(zaak.getResultaat()).getResultaattype());
        if (resultaattype.getArchiefactietermijn() != null) { // no idea what it means when there is no archiefactietermijn
            final LocalDate brondatum = bepaalBrondatum(zaak, resultaattype);
            if (brondatum != null) {
                final Zaak zaakPatch = new Zaak();
                zaakPatch.setArchiefactiedatum(brondatum.plus(Period.parse(resultaattype.getArchiefactietermijn())));
                zrcClientService.patchZaak(zaakUUID, zaakPatch);
            }
        }
    }

    private LocalDate bepaalBrondatum(final Zaak zaak, final ResultaatType resultaattype) {
        final BrondatumArchiefprocedure brondatumArchiefprocedure =
                resultaattype.getBrondatumArchiefprocedure();
        if (brondatumArchiefprocedure != null) {
            switch (brondatumArchiefprocedure.getAfleidingswijze()) {
                case AFGEHANDELD:
                    return zaak.getEinddatum();
                default:
                    LOG.warning(
                            String.format("De brondatum bepaling voor afleidingswijze %s is nog niet geimplementeerd",
                                          brondatumArchiefprocedure.getAfleidingswijze()));
            }
        }
        return null;
    }

    private StatusType readStatustype(final List<StatusType> statustypes, final String omschrijving,
            final URI zaaktypeURI) {
        return statustypes.stream()
                .filter(statustype -> omschrijving.equals(statustype.getOmschrijving()))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Zaaktype '%s': Statustype with omschrijving '%s' not found", zaaktypeURI,
                                      omschrijving)));
    }

    private StatusType readStatustypeEind(final List<StatusType> statustypes,
            final URI zaaktypeURI) {
        return statustypes.stream()
                .filter(StatusType::getIsEindstatus)
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Zaaktype '%s': No eind Status found for Zaaktype.", zaaktypeURI)));
    }
}
