/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertByteArrayToBase64String;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertToEnkelvoudigInformatieObjectDataStatusEnum;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertToEnkelvoudigInformatieObjectStatusEnum;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertToEnkelvoudigInformatieObjectWithLockDataStatusEnum;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertToEnkelvoudigInformatieObjectWithLockDataVertrouwelijkheidaanduidingEnum;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertToVertrouwelijkheidaanduidingEnum;
import static net.atos.client.zgw.shared.util.InformatieobjectenUtil.convertToVertrouwelijkheidaanduidingEnumData;
import static net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI;
import static net.atos.zac.configuratie.ConfiguratieService.OMSCHRIJVING_TAAK_DOCUMENT;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import net.atos.client.zgw.brc.BRCClientService;
import net.atos.client.zgw.drc.DRCClientService;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObject;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObjectData;
import net.atos.client.zgw.drc.model.EnkelvoudigInformatieObjectWithLockData;
import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.zac.app.configuratie.converter.RESTTaalConverter;
import net.atos.zac.app.identity.converter.RESTUserConverter;
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieObjectVersieGegevens;
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieobject;
import net.atos.zac.app.informatieobjecten.model.RESTFileUpload;
import net.atos.zac.app.informatieobjecten.model.RESTGekoppeldeZaakEnkelvoudigInformatieObject;
import net.atos.zac.app.policy.converter.RESTRechtenConverter;
import net.atos.zac.app.taken.model.RESTTaakDocumentData;
import net.atos.zac.app.zaken.model.RelatieType;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.policy.output.DocumentRechten;
import net.atos.zac.util.UriUtil;

public class RESTInformatieobjectConverter {

    @Inject
    private ZTCClientService ztcClientService;

    @Inject
    private DRCClientService drcClientService;

    @Inject
    private BRCClientService brcClientService;

    @Inject
    private ZRCClientService zrcClientService;

    @Inject
    private RESTTaalConverter restTaalConverter;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    @Inject
    private RESTUserConverter restUserConverter;

    @Inject
    private RESTOndertekeningConverter restOndertekeningConverter;

    @Inject
    private EnkelvoudigInformatieObjectLockService enkelvoudigInformatieObjectLockService;

    @Inject
    private IdentityService identityService;

    @Inject
    private RESTRechtenConverter rechtenConverter;

    @Inject
    private PolicyService policyService;

    @Inject
    private ConfiguratieService configuratieService;

    public List<RESTEnkelvoudigInformatieobject> convertToREST(
            final List<ZaakInformatieobject> zaakInformatieobjecten
    ) {
        return zaakInformatieobjecten.stream().map(this::convertToREST).toList();
    }

    public RESTEnkelvoudigInformatieobject convertToREST(final ZaakInformatieobject zaakInformatieObject) {
        final EnkelvoudigInformatieObject enkelvoudigInformatieObject =
                drcClientService.readEnkelvoudigInformatieobject(
                zaakInformatieObject.getInformatieobject());
        final Zaak zaak = zrcClientService.readZaak(zaakInformatieObject.getZaakUUID());
        return convertToREST(enkelvoudigInformatieObject, zaak);
    }

    public RESTEnkelvoudigInformatieobject convertToREST(
            final EnkelvoudigInformatieObject enkelvoudigInformatieObject
    ) {
        return convertToREST(enkelvoudigInformatieObject, null);
    }

    public RESTEnkelvoudigInformatieobject convertToREST(
            final EnkelvoudigInformatieObject enkelvoudigInformatieObject,
            final Zaak zaak
    ) {
        final UUID enkelvoudigInformatieObjectUUID = parseUUIDFromResourceURI(enkelvoudigInformatieObject.getUrl());
        final EnkelvoudigInformatieObjectLock lock = enkelvoudigInformatieObject.getLocked() ?
                enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID).orElse(null)
                : null;
        final DocumentRechten rechten = policyService.readDocumentRechten(enkelvoudigInformatieObject, lock, zaak);
        final RESTEnkelvoudigInformatieobject restEnkelvoudigInformatieobject = new RESTEnkelvoudigInformatieobject();
        restEnkelvoudigInformatieobject.uuid = enkelvoudigInformatieObjectUUID;
        restEnkelvoudigInformatieobject.identificatie = enkelvoudigInformatieObject.getIdentificatie();
        restEnkelvoudigInformatieobject.rechten = rechtenConverter.convert(rechten);
        restEnkelvoudigInformatieobject.isBesluitDocument = brcClientService.isInformatieObjectGekoppeldAanBesluit(enkelvoudigInformatieObject.getUrl());
        if (rechten.getLezen()) {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getTitel();
            restEnkelvoudigInformatieobject.bronorganisatie = enkelvoudigInformatieObject.getBronorganisatie()
                    .equals(ConfiguratieService.BRON_ORGANISATIE) ? null : enkelvoudigInformatieObject.getBronorganisatie();
            restEnkelvoudigInformatieobject.creatiedatum = enkelvoudigInformatieObject.getCreatiedatum();
            if (enkelvoudigInformatieObject.getVertrouwelijkheidaanduiding() != null) {
                restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding = enkelvoudigInformatieObject.getVertrouwelijkheidaanduiding()
                        .toString();
            }
            restEnkelvoudigInformatieobject.auteur = enkelvoudigInformatieObject.getAuteur();
            if (enkelvoudigInformatieObject.getStatus() != null) {
                restEnkelvoudigInformatieobject.status = enkelvoudigInformatieObject.getStatus();
            }
            restEnkelvoudigInformatieobject.formaat = enkelvoudigInformatieObject.getFormaat();

            configuratieService.findTaal(enkelvoudigInformatieObject.getTaal())
                    .ifPresent(taal -> restEnkelvoudigInformatieobject.taal = taal.getNaam());
            restEnkelvoudigInformatieobject.versie = enkelvoudigInformatieObject.getVersie();
            restEnkelvoudigInformatieobject.registratiedatumTijd =
                    enkelvoudigInformatieObject.getBeginRegistratie().toZonedDateTime();
            restEnkelvoudigInformatieobject.bestandsnaam = enkelvoudigInformatieObject.getBestandsnaam();
            if (enkelvoudigInformatieObject.getLink() != null) {
                restEnkelvoudigInformatieobject.link = enkelvoudigInformatieObject.getLink().toString();
            }
            restEnkelvoudigInformatieobject.beschrijving = enkelvoudigInformatieObject.getBeschrijving();
            restEnkelvoudigInformatieobject.ontvangstdatum = enkelvoudigInformatieObject.getOntvangstdatum();
            restEnkelvoudigInformatieobject.verzenddatum = enkelvoudigInformatieObject.getVerzenddatum();
            if (lock != null) {
                restEnkelvoudigInformatieobject.gelockedDoor = restUserConverter.convertUser(
                        identityService.readUser(lock.getUserId()));
            }
            restEnkelvoudigInformatieobject.bestandsomvang =
                    enkelvoudigInformatieObject.getBestandsomvang().longValue();
            restEnkelvoudigInformatieobject.informatieobjectTypeOmschrijving = ztcClientService.readInformatieobjecttype(
                    enkelvoudigInformatieObject.getInformatieobjecttype()).getOmschrijving();
            restEnkelvoudigInformatieobject.informatieobjectTypeUUID =
                    parseUUIDFromResourceURI(enkelvoudigInformatieObject.getInformatieobjecttype());
            if (enkelvoudigInformatieObject.getOndertekening() != null && enkelvoudigInformatieObject.getOndertekening()
                    .getSoort() != null &&
                    enkelvoudigInformatieObject.getOndertekening().getDatum() != null) {
                restEnkelvoudigInformatieobject.ondertekening =
                        restOndertekeningConverter.convert(enkelvoudigInformatieObject.getOndertekening());
            }
        } else {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getIdentificatie();
        }
        return restEnkelvoudigInformatieobject;
    }

    public EnkelvoudigInformatieObjectData convertZaakObject(
            final RESTEnkelvoudigInformatieobject restEnkelvoudigInformatieobject,
            final RESTFileUpload bestand
    ) {
        final EnkelvoudigInformatieObjectData enkelvoudigInformatieobjectWithInhoud =
                new EnkelvoudigInformatieObjectData();
        enkelvoudigInformatieobjectWithInhoud.setBronorganisatie(ConfiguratieService.BRON_ORGANISATIE);
        enkelvoudigInformatieobjectWithInhoud.setCreatiedatum(restEnkelvoudigInformatieobject.creatiedatum);
        enkelvoudigInformatieobjectWithInhoud.setTitel(restEnkelvoudigInformatieobject.titel);
        enkelvoudigInformatieobjectWithInhoud.setAuteur(restEnkelvoudigInformatieobject.auteur);
        enkelvoudigInformatieobjectWithInhoud.setTaal(restEnkelvoudigInformatieobject.taal);
        enkelvoudigInformatieobjectWithInhoud.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID)
                        .getUrl());
        enkelvoudigInformatieobjectWithInhoud.setInhoud(convertByteArrayToBase64String(bestand.file));
        enkelvoudigInformatieobjectWithInhoud.setFormaat(bestand.type);
        enkelvoudigInformatieobjectWithInhoud.setBestandsnaam(restEnkelvoudigInformatieobject.bestandsnaam);
        enkelvoudigInformatieobjectWithInhoud.setBeschrijving(restEnkelvoudigInformatieobject.beschrijving);
        enkelvoudigInformatieobjectWithInhoud.setStatus(
                convertToEnkelvoudigInformatieObjectDataStatusEnum(restEnkelvoudigInformatieobject.status)
        );
        enkelvoudigInformatieobjectWithInhoud.setVerzenddatum(restEnkelvoudigInformatieobject.verzenddatum);
        enkelvoudigInformatieobjectWithInhoud.setOntvangstdatum(restEnkelvoudigInformatieobject.ontvangstdatum);
        enkelvoudigInformatieobjectWithInhoud.setVertrouwelijkheidaanduiding(
                convertToVertrouwelijkheidaanduidingEnumData(restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding)
        );
        return enkelvoudigInformatieobjectWithInhoud;
    }

    public EnkelvoudigInformatieObjectData convertTaakObject(
            final RESTEnkelvoudigInformatieobject restEnkelvoudigInformatieobject,
            final RESTFileUpload bestand
    ) {
        final EnkelvoudigInformatieObjectData enkelvoudigInformatieObjectData = new EnkelvoudigInformatieObjectData();
        enkelvoudigInformatieObjectData.setBronorganisatie(ConfiguratieService.BRON_ORGANISATIE);
        enkelvoudigInformatieObjectData.setCreatiedatum(LocalDate.now());
        enkelvoudigInformatieObjectData.setTitel(restEnkelvoudigInformatieobject.titel);
        enkelvoudigInformatieObjectData.setAuteur(loggedInUserInstance.get().getFullName());
        enkelvoudigInformatieObjectData.setTaal(ConfiguratieService.TAAL_NEDERLANDS);
        enkelvoudigInformatieObjectData.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID)
                        .getUrl());
        enkelvoudigInformatieObjectData.setInhoud(convertByteArrayToBase64String(bestand.file));
        enkelvoudigInformatieObjectData.setFormaat(bestand.type);
        enkelvoudigInformatieObjectData.setBestandsnaam(bestand.filename);
        enkelvoudigInformatieObjectData.setBeschrijving(OMSCHRIJVING_TAAK_DOCUMENT);
        enkelvoudigInformatieObjectData.setStatus(EnkelvoudigInformatieObjectData.StatusEnum.DEFINITIEF);
        enkelvoudigInformatieObjectData.setVerzenddatum(restEnkelvoudigInformatieobject.verzenddatum);
        enkelvoudigInformatieObjectData.setOntvangstdatum(restEnkelvoudigInformatieobject.ontvangstdatum);
        enkelvoudigInformatieObjectData.setVertrouwelijkheidaanduiding(
                EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.OPENBAAR);
        return enkelvoudigInformatieObjectData;
    }

    public EnkelvoudigInformatieObjectData convert(
            final RESTTaakDocumentData documentData,
            final RESTFileUpload bestand
    ) {
        final EnkelvoudigInformatieObjectData enkelvoudigInformatieobjectWithInhoud = new EnkelvoudigInformatieObjectData();
        enkelvoudigInformatieobjectWithInhoud.setBronorganisatie(ConfiguratieService.BRON_ORGANISATIE);
        enkelvoudigInformatieobjectWithInhoud.setCreatiedatum(LocalDate.now());
        enkelvoudigInformatieobjectWithInhoud.setTitel(documentData.documentTitel);
        enkelvoudigInformatieobjectWithInhoud.setAuteur(loggedInUserInstance.get().getFullName());
        enkelvoudigInformatieobjectWithInhoud.setTaal(ConfiguratieService.TAAL_NEDERLANDS);
        enkelvoudigInformatieobjectWithInhoud.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(documentData.documentType.uuid).getUrl());
        enkelvoudigInformatieobjectWithInhoud.setInhoud(convertByteArrayToBase64String(bestand.file));
        enkelvoudigInformatieobjectWithInhoud.setFormaat(bestand.type);
        enkelvoudigInformatieobjectWithInhoud.setBestandsnaam(bestand.filename);
        enkelvoudigInformatieobjectWithInhoud.setStatus(EnkelvoudigInformatieObjectData.StatusEnum.DEFINITIEF);
        enkelvoudigInformatieobjectWithInhoud.setVertrouwelijkheidaanduiding(
                EnkelvoudigInformatieObjectData.VertrouwelijkheidaanduidingEnum.valueOf(documentData.documentType.vertrouwelijkheidaanduiding)
        );
        return enkelvoudigInformatieobjectWithInhoud;
    }


    public RESTEnkelvoudigInformatieObjectVersieGegevens convertToRESTEnkelvoudigInformatieObjectVersieGegevens(
            final EnkelvoudigInformatieObject informatieobject) {
        final RESTEnkelvoudigInformatieObjectVersieGegevens restEnkelvoudigInformatieObjectVersieGegevens = new RESTEnkelvoudigInformatieObjectVersieGegevens();

        restEnkelvoudigInformatieObjectVersieGegevens.uuid = UriUtil.uuidFromURI(informatieobject.getUrl());

        if (informatieobject.getStatus() != null) {
            restEnkelvoudigInformatieObjectVersieGegevens.status = informatieobject.getStatus();
        }
        if (informatieobject.getVertrouwelijkheidaanduiding() != null) {
            restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding =
                    informatieobject.getVertrouwelijkheidaanduiding().value();
        }

        restEnkelvoudigInformatieObjectVersieGegevens.beschrijving = informatieobject.getBeschrijving();
        restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum = informatieobject.getVerzenddatum();
        restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum = informatieobject.getOntvangstdatum();
        restEnkelvoudigInformatieObjectVersieGegevens.titel = informatieobject.getTitel();
        restEnkelvoudigInformatieObjectVersieGegevens.auteur = informatieobject.getAuteur();
        configuratieService.findTaal(informatieobject.getTaal())
                .map(restTaalConverter::convert)
                .ifPresent(taal -> restEnkelvoudigInformatieObjectVersieGegevens.taal = taal);
        restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam = informatieobject.getInhoud().toString();

        return restEnkelvoudigInformatieObjectVersieGegevens;
    }

    public EnkelvoudigInformatieObjectWithLockData convert(
            final RESTEnkelvoudigInformatieObjectVersieGegevens restEnkelvoudigInformatieObjectVersieGegevens,
            final RESTFileUpload file
    ) {
        final EnkelvoudigInformatieObjectWithLockData enkelvoudigInformatieObjectWithLockData =
                new EnkelvoudigInformatieObjectWithLockData();

        if (restEnkelvoudigInformatieObjectVersieGegevens.status != null) {
            enkelvoudigInformatieObjectWithLockData.setStatus(
                    convertToEnkelvoudigInformatieObjectWithLockDataStatusEnum(
                            restEnkelvoudigInformatieObjectVersieGegevens.status
                    )
            );
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding != null) {
            enkelvoudigInformatieObjectWithLockData.setVertrouwelijkheidaanduiding(
                    convertToEnkelvoudigInformatieObjectWithLockDataVertrouwelijkheidaanduidingEnum(
                            restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding
                    )
            );
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.beschrijving != null) {
            enkelvoudigInformatieObjectWithLockData.setBeschrijving(
                    restEnkelvoudigInformatieObjectVersieGegevens.beschrijving);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum != null) {
            enkelvoudigInformatieObjectWithLockData.setVerzenddatum(
                    restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum != null) {
            enkelvoudigInformatieObjectWithLockData.setOntvangstdatum(
                    restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.titel != null) {
            enkelvoudigInformatieObjectWithLockData.setTitel(restEnkelvoudigInformatieObjectVersieGegevens.titel);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.taal != null) {
            enkelvoudigInformatieObjectWithLockData.setTaal(
                    restEnkelvoudigInformatieObjectVersieGegevens.taal.code);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.auteur != null) {
            enkelvoudigInformatieObjectWithLockData.setAuteur(
                    restEnkelvoudigInformatieObjectVersieGegevens.auteur);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam != null) {
            enkelvoudigInformatieObjectWithLockData.setBestandsnaam(
                    (restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam));
        }
        if (file != null && file.file != null) {
            enkelvoudigInformatieObjectWithLockData.setInhoud(convertByteArrayToBase64String(file.file));
            enkelvoudigInformatieObjectWithLockData.setBestandsomvang(file.file.length);
            enkelvoudigInformatieObjectWithLockData.setFormaat(file.type);
        }

        return enkelvoudigInformatieObjectWithLockData;
    }

    public List<RESTEnkelvoudigInformatieobject> convertUUIDsToREST(final List<UUID> enkelvoudigInformatieobjectUUIDs,
            final Zaak zaak) {
        return enkelvoudigInformatieobjectUUIDs.stream()
                .map(enkelvoudigInformatieobjectUUID -> convertToREST(
                        drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID), zaak))
                .toList();
    }

    public RESTGekoppeldeZaakEnkelvoudigInformatieObject convertToREST(
            final ZaakInformatieobject zaakInformatieObject,
            final RelatieType relatieType,
            final Zaak zaak
    ) {
        final EnkelvoudigInformatieObject enkelvoudigInformatieObject =
                drcClientService.readEnkelvoudigInformatieobject(
                zaakInformatieObject.getInformatieobject());
        final UUID enkelvoudigInformatieObjectUUID =
                parseUUIDFromResourceURI(enkelvoudigInformatieObject.getUrl());
        final EnkelvoudigInformatieObjectLock lock = enkelvoudigInformatieObject.getLocked() ?
                enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID).orElse(null)
                : null;
        final DocumentRechten rechten = policyService.readDocumentRechten(enkelvoudigInformatieObject, lock, zaak);
        final RESTGekoppeldeZaakEnkelvoudigInformatieObject restEnkelvoudigInformatieobject = new RESTGekoppeldeZaakEnkelvoudigInformatieObject();
        restEnkelvoudigInformatieobject.uuid = enkelvoudigInformatieObjectUUID;
        restEnkelvoudigInformatieobject.identificatie = enkelvoudigInformatieObject.getIdentificatie();
        restEnkelvoudigInformatieobject.rechten = rechtenConverter.convert(rechten);
        if (rechten.getLezen()) {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getTitel();
            restEnkelvoudigInformatieobject.bronorganisatie = enkelvoudigInformatieObject.getBronorganisatie()
                    .equals(ConfiguratieService.BRON_ORGANISATIE) ? null : enkelvoudigInformatieObject.getBronorganisatie();
            restEnkelvoudigInformatieobject.creatiedatum = enkelvoudigInformatieObject.getCreatiedatum();
            if (enkelvoudigInformatieObject.getVertrouwelijkheidaanduiding() != null) {
                restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding = enkelvoudigInformatieObject.getVertrouwelijkheidaanduiding()
                        .toString();
            }
            restEnkelvoudigInformatieobject.auteur = enkelvoudigInformatieObject.getAuteur();
            if (enkelvoudigInformatieObject.getStatus() != null) {
                restEnkelvoudigInformatieobject.status = enkelvoudigInformatieObject.getStatus();
            }
            restEnkelvoudigInformatieobject.formaat = enkelvoudigInformatieObject.getFormaat();
            configuratieService.findTaal(enkelvoudigInformatieObject.getTaal())
                    .ifPresent(taal -> restEnkelvoudigInformatieobject.taal = taal.getName());
            restEnkelvoudigInformatieobject.versie = enkelvoudigInformatieObject.getVersie();
            restEnkelvoudigInformatieobject.registratiedatumTijd =
                    enkelvoudigInformatieObject.getBeginRegistratie().toZonedDateTime();
            restEnkelvoudigInformatieobject.bestandsnaam = enkelvoudigInformatieObject.getBestandsnaam();
            if (enkelvoudigInformatieObject.getLink() != null) {
                restEnkelvoudigInformatieobject.link = enkelvoudigInformatieObject.getLink().toString();
            }
            restEnkelvoudigInformatieobject.beschrijving = enkelvoudigInformatieObject.getBeschrijving();
            restEnkelvoudigInformatieobject.ontvangstdatum = enkelvoudigInformatieObject.getOntvangstdatum();
            restEnkelvoudigInformatieobject.verzenddatum = enkelvoudigInformatieObject.getVerzenddatum();
            if (lock != null) {
                restEnkelvoudigInformatieobject.gelockedDoor = restUserConverter.convertUser(
                        identityService.readUser(lock.getUserId()));
            }
            restEnkelvoudigInformatieobject.bestandsomvang =
                    enkelvoudigInformatieObject.getBestandsomvang().longValue();
            restEnkelvoudigInformatieobject.informatieobjectTypeOmschrijving = ztcClientService.readInformatieobjecttype(
                    enkelvoudigInformatieObject.getInformatieobjecttype()).getOmschrijving();
            restEnkelvoudigInformatieobject.informatieobjectTypeUUID =
                    parseUUIDFromResourceURI(enkelvoudigInformatieObject.getInformatieobjecttype());
            restEnkelvoudigInformatieobject.relatieType = relatieType;
            restEnkelvoudigInformatieobject.zaakIdentificatie = zaak.getIdentificatie();
            restEnkelvoudigInformatieobject.zaakUUID = zaak.getUuid();
        } else {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getIdentificatie();
        }
        return restEnkelvoudigInformatieobject;
    }

    public List<RESTEnkelvoudigInformatieobject> convertInformatieobjectenToREST(
            final List<EnkelvoudigInformatieObject> informatieobjecten) {
        return informatieobjecten.stream().map(this::convertToREST).toList();
    }

    /**
     * Utility function to convert a {@link EnkelvoudigInformatieObjectWithLockData} object
     * to a {@link EnkelvoudigInformatieObject} object.
     * <br>
     * Eventhough they both contain for the most part the exact same fields the OpenAPI
     * Generator generates two separate Java classes.
     *
     * @param enkelvoudigInformatieObjectWithLockData the object to be converted
     * @return the converted object
     */
    public static EnkelvoudigInformatieObject convertToEnkelvoudigInformatieObject(
            EnkelvoudigInformatieObjectWithLockData enkelvoudigInformatieObjectWithLockData
    ) {
        final EnkelvoudigInformatieObject enkelvoudigInformatieObject = new EnkelvoudigInformatieObject();
        enkelvoudigInformatieObject.setAuteur(enkelvoudigInformatieObjectWithLockData.getAuteur());
        enkelvoudigInformatieObject.setBeschrijving(enkelvoudigInformatieObjectWithLockData.getBeschrijving());
        enkelvoudigInformatieObject.setBestandsomvang(
                enkelvoudigInformatieObjectWithLockData.getBestandsomvang()
        );
        enkelvoudigInformatieObject.setBestandsnaam(enkelvoudigInformatieObjectWithLockData.getBestandsnaam());
        enkelvoudigInformatieObject.setBronorganisatie(
                enkelvoudigInformatieObjectWithLockData.getBronorganisatie());
        enkelvoudigInformatieObject.setCreatiedatum(enkelvoudigInformatieObjectWithLockData.getCreatiedatum());
        enkelvoudigInformatieObject.setFormaat(enkelvoudigInformatieObjectWithLockData.getFormaat());
        enkelvoudigInformatieObject.setIdentificatie(enkelvoudigInformatieObjectWithLockData.getIdentificatie());
        enkelvoudigInformatieObject.setIndicatieGebruiksrecht(enkelvoudigInformatieObjectWithLockData.getIndicatieGebruiksrecht());
        enkelvoudigInformatieObject.setInformatieobjecttype(enkelvoudigInformatieObjectWithLockData.getInformatieobjecttype());
        enkelvoudigInformatieObject.setIntegriteit(
                enkelvoudigInformatieObjectWithLockData.getIntegriteit()
        );
        enkelvoudigInformatieObject.setLink(enkelvoudigInformatieObjectWithLockData.getLink());
        enkelvoudigInformatieObject.setOndertekening(
                enkelvoudigInformatieObjectWithLockData.getOndertekening()
        );
        enkelvoudigInformatieObject.setOntvangstdatum(enkelvoudigInformatieObjectWithLockData.getOntvangstdatum());
        enkelvoudigInformatieObject.setStatus(
                convertToEnkelvoudigInformatieObjectStatusEnum(
                    enkelvoudigInformatieObjectWithLockData.getStatus()
                )
        );
        enkelvoudigInformatieObject.setTaal(enkelvoudigInformatieObjectWithLockData.getTaal());
        enkelvoudigInformatieObject.setTitel(enkelvoudigInformatieObjectWithLockData.getTitel());
        enkelvoudigInformatieObject.setVerschijningsvorm(enkelvoudigInformatieObjectWithLockData.getVerschijningsvorm());
        enkelvoudigInformatieObject.setVertrouwelijkheidaanduiding(
                convertToVertrouwelijkheidaanduidingEnum(
                        enkelvoudigInformatieObjectWithLockData.getVertrouwelijkheidaanduiding().value()
                )
        );
        enkelvoudigInformatieObject.setVerzenddatum(enkelvoudigInformatieObjectWithLockData.getVerzenddatum());
        return enkelvoudigInformatieObject;
    }
}
