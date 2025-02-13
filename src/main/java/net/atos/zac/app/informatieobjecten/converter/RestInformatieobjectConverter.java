/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.converter;

import static net.atos.client.zgw.util.UriUtilsKt.extractUuid;
import static net.atos.zac.app.configuratie.model.RestTaalKt.toRestTaal;
import static net.atos.zac.app.identity.model.RestUserKt.toRestUser;
import static net.atos.zac.configuratie.ConfiguratieService.OMSCHRIJVING_TAAK_DOCUMENT;
import static net.atos.zac.identity.model.UserKt.getFullName;
import static nl.info.zac.util.Base64ConvertersKt.toBase64String;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import net.atos.client.zgw.brc.BrcClientService;
import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest;
import net.atos.client.zgw.drc.model.generated.StatusEnum;
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum;
import net.atos.client.zgw.shared.exception.ZgwErrorException;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.zac.app.informatieobjecten.model.RESTFileUpload;
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieObjectVersieGegevens;
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject;
import net.atos.zac.app.informatieobjecten.model.RestGekoppeldeZaakEnkelvoudigInformatieObject;
import net.atos.zac.app.policy.converter.RestRechtenConverter;
import net.atos.zac.app.task.model.RestTaskDocumentData;
import net.atos.zac.app.zaak.model.RelatieType;
import net.atos.zac.authentication.LoggedInUser;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.configuratie.model.Taal;
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.policy.output.DocumentRechten;

public class RestInformatieobjectConverter {
    private static final Logger LOG = Logger.getLogger(RestInformatieobjectConverter.class.getName());

    private BrcClientService brcClientService;
    private ConfiguratieService configuratieService;
    private DrcClientService drcClientService;
    private EnkelvoudigInformatieObjectLockService enkelvoudigInformatieObjectLockService;
    private IdentityService identityService;
    private Instance<LoggedInUser> loggedInUserInstance;
    private PolicyService policyService;
    private ZrcClientService zrcClientService;
    private ZtcClientService ztcClientService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public RestInformatieobjectConverter() {
    }

    @Inject
    public RestInformatieobjectConverter(
            BrcClientService brcClientService,
            ConfiguratieService configuratieService,
            DrcClientService drcClientService,
            EnkelvoudigInformatieObjectLockService enkelvoudigInformatieObjectLockService,
            IdentityService identityService,
            Instance<LoggedInUser> loggedInUserInstance,
            PolicyService policyService,
            ZrcClientService zrcClientService,
            ZtcClientService ztcClientService
    ) {
        this.ztcClientService = ztcClientService;
        this.drcClientService = drcClientService;
        this.brcClientService = brcClientService;
        this.zrcClientService = zrcClientService;
        this.loggedInUserInstance = loggedInUserInstance;
        this.enkelvoudigInformatieObjectLockService = enkelvoudigInformatieObjectLockService;
        this.identityService = identityService;
        this.policyService = policyService;
        this.configuratieService = configuratieService;
    }

    public List<RestEnkelvoudigInformatieobject> convertToREST(
            final List<ZaakInformatieobject> zaakInformatieobjecten
    ) {
        return zaakInformatieobjecten.stream().map(this::convertToREST).toList();
    }

    public RestEnkelvoudigInformatieobject convertToREST(final ZaakInformatieobject zaakInformatieObject) {
        final EnkelvoudigInformatieObject enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
                zaakInformatieObject.getInformatieobject()
        );
        final Zaak zaak = zrcClientService.readZaak(zaakInformatieObject.getZaakUUID());
        return convertToREST(enkelvoudigInformatieObject, zaak);
    }

    public RestEnkelvoudigInformatieobject convertToREST(
            final EnkelvoudigInformatieObject enkelvoudigInformatieObject
    ) {
        return convertToREST(enkelvoudigInformatieObject, null);
    }

    public RestEnkelvoudigInformatieobject convertToREST(
            final EnkelvoudigInformatieObject enkelvoudigInformatieObject,
            final Zaak zaak
    ) {
        final UUID enkelvoudigInformatieObjectUUID = extractUuid(enkelvoudigInformatieObject.getUrl());
        final EnkelvoudigInformatieObjectLock lock = enkelvoudigInformatieObject.getLocked() ?
                enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID) : null;
        final DocumentRechten rechten = policyService.readDocumentRechten(enkelvoudigInformatieObject, lock, zaak);
        final RestEnkelvoudigInformatieobject restEnkelvoudigInformatieobject = new RestEnkelvoudigInformatieobject();
        restEnkelvoudigInformatieobject.uuid = enkelvoudigInformatieObjectUUID;
        restEnkelvoudigInformatieobject.identificatie = enkelvoudigInformatieObject.getIdentificatie();
        restEnkelvoudigInformatieobject.rechten = RestRechtenConverter.convert(rechten);
        restEnkelvoudigInformatieobject.isBesluitDocument = brcClientService.isInformatieObjectGekoppeldAanBesluit(
                enkelvoudigInformatieObject.getUrl()
        );
        if (rechten.lezen()) {
            convertEnkelvoudigInformatieObject(enkelvoudigInformatieObject, lock, restEnkelvoudigInformatieobject);
            if (
                enkelvoudigInformatieObject.getOndertekening() != null &&
                enkelvoudigInformatieObject.getOndertekening().getSoort() != null &&
                enkelvoudigInformatieObject.getOndertekening().getDatum() != null
            ) {
                restEnkelvoudigInformatieobject.ondertekening = RestOndertekeningConverter.convert(
                        enkelvoudigInformatieObject.getOndertekening()
                );
            }
        } else {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getIdentificatie();
        }
        return restEnkelvoudigInformatieobject;
    }

    private void convertEnkelvoudigInformatieObject(
            EnkelvoudigInformatieObject enkelvoudigInformatieObject,
            EnkelvoudigInformatieObjectLock lock,
            RestEnkelvoudigInformatieobject restEnkelvoudigInformatieobject
    ) {
        restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getTitel();
        if (enkelvoudigInformatieObject.getBronorganisatie() != null) {
            restEnkelvoudigInformatieobject.bronorganisatie = enkelvoudigInformatieObject.getBronorganisatie()
                    .equals(configuratieService.readBronOrganisatie()) ? null : enkelvoudigInformatieObject.getBronorganisatie();
        }
        restEnkelvoudigInformatieobject.creatiedatum = enkelvoudigInformatieObject.getCreatiedatum();
        if (enkelvoudigInformatieObject.getVertrouwelijkheidaanduiding() != null) {
            // we use the uppercase version of this enum in the ZAC backend API
            restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding = enkelvoudigInformatieObject.getVertrouwelijkheidaanduiding()
                    .name();
        }
        restEnkelvoudigInformatieobject.auteur = enkelvoudigInformatieObject.getAuteur();
        if (enkelvoudigInformatieObject.getStatus() != null) {
            restEnkelvoudigInformatieobject.status = enkelvoudigInformatieObject.getStatus();
        }
        restEnkelvoudigInformatieobject.formaat = enkelvoudigInformatieObject.getFormaat();

        final Taal taal = configuratieService.findTaal(enkelvoudigInformatieObject.getTaal());
        if (taal != null) {
            restEnkelvoudigInformatieobject.taal = taal.naam;
        }

        restEnkelvoudigInformatieobject.versie = enkelvoudigInformatieObject.getVersie();
        restEnkelvoudigInformatieobject.registratiedatumTijd = enkelvoudigInformatieObject.getBeginRegistratie().toZonedDateTime();
        restEnkelvoudigInformatieobject.bestandsnaam = enkelvoudigInformatieObject.getBestandsnaam();
        if (enkelvoudigInformatieObject.getLink() != null) {
            restEnkelvoudigInformatieobject.link = enkelvoudigInformatieObject.getLink().toString();
        }
        restEnkelvoudigInformatieobject.beschrijving = enkelvoudigInformatieObject.getBeschrijving();
        restEnkelvoudigInformatieobject.ontvangstdatum = enkelvoudigInformatieObject.getOntvangstdatum();
        restEnkelvoudigInformatieobject.verzenddatum = enkelvoudigInformatieObject.getVerzenddatum();
        if (lock != null) {
            restEnkelvoudigInformatieobject.gelockedDoor = toRestUser(identityService.readUser(lock.getUserId()));
        }
        restEnkelvoudigInformatieobject.bestandsomvang = enkelvoudigInformatieObject.getBestandsomvang() != null ?
                enkelvoudigInformatieObject.getBestandsomvang().longValue() : 0;
        restEnkelvoudigInformatieobject.informatieobjectTypeOmschrijving = ztcClientService.readInformatieobjecttype(
                enkelvoudigInformatieObject.getInformatieobjecttype()).getOmschrijving();
        restEnkelvoudigInformatieobject.informatieobjectTypeUUID = extractUuid(enkelvoudigInformatieObject
                .getInformatieobjecttype());
    }

    public EnkelvoudigInformatieObjectCreateLockRequest convertZaakObject(
            final RestEnkelvoudigInformatieobject restEnkelvoudigInformatieobject
    ) {
        final EnkelvoudigInformatieObjectCreateLockRequest enkelvoudigInformatieObjectCreateLockRequest = buildEnkelvoudigInformatieObjectData(
                restEnkelvoudigInformatieobject
        );
        enkelvoudigInformatieObjectCreateLockRequest.setInhoud(toBase64String(restEnkelvoudigInformatieobject.file));
        enkelvoudigInformatieObjectCreateLockRequest.setBestandsomvang(restEnkelvoudigInformatieobject.file.length);
        enkelvoudigInformatieObjectCreateLockRequest.setFormaat(restEnkelvoudigInformatieobject.formaat);
        return enkelvoudigInformatieObjectCreateLockRequest;
    }

    @NotNull
    private EnkelvoudigInformatieObjectCreateLockRequest buildEnkelvoudigInformatieObjectData(
            RestEnkelvoudigInformatieobject restEnkelvoudigInformatieobject
    ) {
        final EnkelvoudigInformatieObjectCreateLockRequest enkelvoudigInformatieobjectWithInhoud = new EnkelvoudigInformatieObjectCreateLockRequest();
        enkelvoudigInformatieobjectWithInhoud.setBronorganisatie(configuratieService.readBronOrganisatie());
        enkelvoudigInformatieobjectWithInhoud.setCreatiedatum(restEnkelvoudigInformatieobject.creatiedatum);
        enkelvoudigInformatieobjectWithInhoud.setTitel(restEnkelvoudigInformatieobject.titel);
        enkelvoudigInformatieobjectWithInhoud.setAuteur(restEnkelvoudigInformatieobject.auteur);
        enkelvoudigInformatieobjectWithInhoud.setTaal(restEnkelvoudigInformatieobject.taal);
        enkelvoudigInformatieobjectWithInhoud.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID).getUrl()
        );
        enkelvoudigInformatieobjectWithInhoud.setBestandsnaam(restEnkelvoudigInformatieobject.bestandsnaam);
        enkelvoudigInformatieobjectWithInhoud.setBeschrijving(restEnkelvoudigInformatieobject.beschrijving);
        enkelvoudigInformatieobjectWithInhoud.setStatus(restEnkelvoudigInformatieobject.status);
        enkelvoudigInformatieobjectWithInhoud.setVerzenddatum(restEnkelvoudigInformatieobject.verzenddatum);
        enkelvoudigInformatieobjectWithInhoud.setOntvangstdatum(restEnkelvoudigInformatieobject.ontvangstdatum);
        enkelvoudigInformatieobjectWithInhoud.setVertrouwelijkheidaanduiding(
                VertrouwelijkheidaanduidingEnum.valueOf(
                        // the values of the enums generated by OpenAPI Generator are the
                        // uppercase variants of the strings used in the APIs
                        restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding.toUpperCase()
                )
        );
        return enkelvoudigInformatieobjectWithInhoud;
    }

    public EnkelvoudigInformatieObjectCreateLockRequest convertTaakObject(
            final RestEnkelvoudigInformatieobject restEnkelvoudigInformatieobject
    ) {
        final EnkelvoudigInformatieObjectCreateLockRequest enkelvoudigInformatieObjectCreateLockRequest = buildTaakEnkelvoudigInformatieObjectData(
                restEnkelvoudigInformatieobject
        );
        enkelvoudigInformatieObjectCreateLockRequest.setInhoud(toBase64String(restEnkelvoudigInformatieobject.file));
        enkelvoudigInformatieObjectCreateLockRequest.setBestandsnaam(restEnkelvoudigInformatieobject.bestandsnaam);
        enkelvoudigInformatieObjectCreateLockRequest.setBestandsomvang(restEnkelvoudigInformatieobject.file.length);
        enkelvoudigInformatieObjectCreateLockRequest.setFormaat(restEnkelvoudigInformatieobject.formaat);
        return enkelvoudigInformatieObjectCreateLockRequest;
    }

    @NotNull
    private EnkelvoudigInformatieObjectCreateLockRequest buildTaakEnkelvoudigInformatieObjectData(
            RestEnkelvoudigInformatieobject restEnkelvoudigInformatieobject
    ) {
        final EnkelvoudigInformatieObjectCreateLockRequest enkelvoudigInformatieObjectData = new EnkelvoudigInformatieObjectCreateLockRequest();
        enkelvoudigInformatieObjectData.setBronorganisatie(configuratieService.readBronOrganisatie());
        enkelvoudigInformatieObjectData.setCreatiedatum(LocalDate.now());
        enkelvoudigInformatieObjectData.setTitel(restEnkelvoudigInformatieobject.titel);
        enkelvoudigInformatieObjectData.setAuteur(getFullName(loggedInUserInstance.get()));
        enkelvoudigInformatieObjectData.setTaal(ConfiguratieService.TAAL_NEDERLANDS);
        enkelvoudigInformatieObjectData.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID).getUrl()
        );
        enkelvoudigInformatieObjectData.setBeschrijving(OMSCHRIJVING_TAAK_DOCUMENT);
        enkelvoudigInformatieObjectData.setStatus(StatusEnum.DEFINITIEF);
        enkelvoudigInformatieObjectData.setVerzenddatum(restEnkelvoudigInformatieobject.verzenddatum);
        enkelvoudigInformatieObjectData.setOntvangstdatum(restEnkelvoudigInformatieobject.ontvangstdatum);
        enkelvoudigInformatieObjectData.setVertrouwelijkheidaanduiding(
                VertrouwelijkheidaanduidingEnum.OPENBAAR
        );
        return enkelvoudigInformatieObjectData;
    }

    public EnkelvoudigInformatieObjectCreateLockRequest convert(
            final RestTaskDocumentData documentData,
            final RESTFileUpload bestand
    ) {
        final EnkelvoudigInformatieObjectCreateLockRequest enkelvoudigInformatieobjectWithInhoud = new EnkelvoudigInformatieObjectCreateLockRequest();
        enkelvoudigInformatieobjectWithInhoud.setBronorganisatie(configuratieService.readBronOrganisatie());
        enkelvoudigInformatieobjectWithInhoud.setCreatiedatum(LocalDate.now());
        enkelvoudigInformatieobjectWithInhoud.setTitel(documentData.getDocumentTitel());
        enkelvoudigInformatieobjectWithInhoud.setAuteur(getFullName(loggedInUserInstance.get()));
        enkelvoudigInformatieobjectWithInhoud.setTaal(ConfiguratieService.TAAL_NEDERLANDS);
        enkelvoudigInformatieobjectWithInhoud.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(documentData.getDocumentType().uuid).getUrl()
        );
        enkelvoudigInformatieobjectWithInhoud.setInhoud(toBase64String(bestand.file));
        enkelvoudigInformatieobjectWithInhoud.setFormaat(bestand.type);
        enkelvoudigInformatieobjectWithInhoud.setBestandsnaam(bestand.filename);
        enkelvoudigInformatieobjectWithInhoud.setStatus(StatusEnum.DEFINITIEF);
        enkelvoudigInformatieobjectWithInhoud.setVertrouwelijkheidaanduiding(
                VertrouwelijkheidaanduidingEnum.valueOf(documentData.getDocumentType().vertrouwelijkheidaanduiding)
        );
        return enkelvoudigInformatieobjectWithInhoud;
    }


    public RestEnkelvoudigInformatieObjectVersieGegevens convertToRestEnkelvoudigInformatieObjectVersieGegevens(
            final EnkelvoudigInformatieObject informatieobject
    ) {
        final RestEnkelvoudigInformatieObjectVersieGegevens restEnkelvoudigInformatieObjectVersieGegevens = new RestEnkelvoudigInformatieObjectVersieGegevens();

        restEnkelvoudigInformatieObjectVersieGegevens.uuid = extractUuid(informatieobject.getUrl());

        if (informatieobject.getStatus() != null) {
            restEnkelvoudigInformatieObjectVersieGegevens.status = informatieobject.getStatus();
        }
        if (informatieobject.getVertrouwelijkheidaanduiding() != null) {
            // we use the uppercase version of this enum in the ZAC backend API
            restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding = informatieobject.getVertrouwelijkheidaanduiding()
                    .name();
        }

        restEnkelvoudigInformatieObjectVersieGegevens.beschrijving = informatieobject.getBeschrijving();
        restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum = informatieobject.getVerzenddatum();
        restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum = informatieobject.getOntvangstdatum();
        restEnkelvoudigInformatieObjectVersieGegevens.titel = informatieobject.getTitel();
        restEnkelvoudigInformatieObjectVersieGegevens.auteur = informatieobject.getAuteur();
        final Taal taal = configuratieService.findTaal(informatieobject.getTaal());
        if (taal != null) {
            restEnkelvoudigInformatieObjectVersieGegevens.taal = toRestTaal(taal);
        }
        restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam = informatieobject.getInhoud().toString();
        restEnkelvoudigInformatieObjectVersieGegevens.informatieobjectTypeUUID = extractUuid(informatieobject
                .getInformatieobjecttype());

        return restEnkelvoudigInformatieObjectVersieGegevens;
    }

    public EnkelvoudigInformatieObjectWithLockRequest convert(
            final RestEnkelvoudigInformatieObjectVersieGegevens restEnkelvoudigInformatieObjectVersieGegevens
    ) {
        final EnkelvoudigInformatieObjectWithLockRequest enkelvoudigInformatieObjectWithLockRequest = createEnkelvoudigInformatieObjectWithLockData(
                restEnkelvoudigInformatieObjectVersieGegevens);
        if (
            restEnkelvoudigInformatieObjectVersieGegevens.file != null &&
            restEnkelvoudigInformatieObjectVersieGegevens.file.length > 0 &&
            restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam != null &&
            restEnkelvoudigInformatieObjectVersieGegevens.formaat != null
        ) {
            enkelvoudigInformatieObjectWithLockRequest.setInhoud(toBase64String(restEnkelvoudigInformatieObjectVersieGegevens.file));
            enkelvoudigInformatieObjectWithLockRequest.setBestandsnaam(restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam);
            enkelvoudigInformatieObjectWithLockRequest.setBestandsomvang(restEnkelvoudigInformatieObjectVersieGegevens.file.length);
            enkelvoudigInformatieObjectWithLockRequest.setFormaat(restEnkelvoudigInformatieObjectVersieGegevens.formaat);
        }

        enkelvoudigInformatieObjectWithLockRequest.setInformatieobjecttype(
                ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieObjectVersieGegevens.informatieobjectTypeUUID).getUrl()
        );

        return enkelvoudigInformatieObjectWithLockRequest;
    }

    private static EnkelvoudigInformatieObjectWithLockRequest createEnkelvoudigInformatieObjectWithLockData(
            RestEnkelvoudigInformatieObjectVersieGegevens restEnkelvoudigInformatieObjectVersieGegevens
    ) {
        final EnkelvoudigInformatieObjectWithLockRequest enkelvoudigInformatieObjectWithLockData = new EnkelvoudigInformatieObjectWithLockRequest();

        if (restEnkelvoudigInformatieObjectVersieGegevens.status != null) {
            enkelvoudigInformatieObjectWithLockData.setStatus(restEnkelvoudigInformatieObjectVersieGegevens.status);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding != null) {
            enkelvoudigInformatieObjectWithLockData.setVertrouwelijkheidaanduiding(
                    // convert this enum to uppercase in case the client sends it in lowercase
                    VertrouwelijkheidaanduidingEnum.valueOf(restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding
                            .toUpperCase())
            );
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.beschrijving != null) {
            enkelvoudigInformatieObjectWithLockData.setBeschrijving(restEnkelvoudigInformatieObjectVersieGegevens.beschrijving);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum != null) {
            enkelvoudigInformatieObjectWithLockData.setVerzenddatum(restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum != null) {
            enkelvoudigInformatieObjectWithLockData.setOntvangstdatum(restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.titel != null) {
            enkelvoudigInformatieObjectWithLockData.setTitel(restEnkelvoudigInformatieObjectVersieGegevens.titel);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.taal != null) {
            enkelvoudigInformatieObjectWithLockData.setTaal(restEnkelvoudigInformatieObjectVersieGegevens.taal.getCode());
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.auteur != null) {
            enkelvoudigInformatieObjectWithLockData.setAuteur(restEnkelvoudigInformatieObjectVersieGegevens.auteur);
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam != null) {
            enkelvoudigInformatieObjectWithLockData.setBestandsnaam((restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam));
        }
        return enkelvoudigInformatieObjectWithLockData;
    }

    public List<RestEnkelvoudigInformatieobject> convertUUIDsToREST(
            final List<UUID> enkelvoudigInformatieobjectUUIDs,
            final Zaak zaak
    ) {
        return enkelvoudigInformatieobjectUUIDs.stream()
                .map(enkelvoudigInformatieobjectUUID -> {
                    try {
                        return convertToREST(
                                drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID),
                                zaak
                        );
                    } catch (ZgwErrorException e) {
                        if (e.getZgwError().getStatus() != HttpStatus.NOT_FOUND_404) {
                            throw e;
                        }
                        LOG.severe(() -> "Document niet gevonden: %s".formatted(enkelvoudigInformatieobjectUUID));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public RestGekoppeldeZaakEnkelvoudigInformatieObject convertToREST(
            final ZaakInformatieobject zaakInformatieObject,
            final RelatieType relatieType,
            final Zaak zaak
    ) {
        final EnkelvoudigInformatieObject enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
                zaakInformatieObject.getInformatieobject());
        final UUID enkelvoudigInformatieObjectUUID = extractUuid(enkelvoudigInformatieObject.getUrl());
        final EnkelvoudigInformatieObjectLock lock = enkelvoudigInformatieObject.getLocked() ?
                enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID) : null;
        final DocumentRechten rechten = policyService.readDocumentRechten(enkelvoudigInformatieObject, lock, zaak);
        final RestGekoppeldeZaakEnkelvoudigInformatieObject restEnkelvoudigInformatieobject = new RestGekoppeldeZaakEnkelvoudigInformatieObject();
        restEnkelvoudigInformatieobject.uuid = enkelvoudigInformatieObjectUUID;
        restEnkelvoudigInformatieobject.identificatie = enkelvoudigInformatieObject.getIdentificatie();
        restEnkelvoudigInformatieobject.rechten = RestRechtenConverter.convert(rechten);
        if (rechten.lezen()) {
            convertEnkelvoudigInformatieObject(enkelvoudigInformatieObject, lock, restEnkelvoudigInformatieobject);
            restEnkelvoudigInformatieobject.relatieType = relatieType;
            restEnkelvoudigInformatieobject.zaakIdentificatie = zaak.getIdentificatie();
            restEnkelvoudigInformatieobject.zaakUUID = zaak.getUuid();
        } else {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.getIdentificatie();
        }
        return restEnkelvoudigInformatieobject;
    }

    public List<RestEnkelvoudigInformatieobject> convertInformatieobjectenToREST(
            final List<EnkelvoudigInformatieObject> informatieobjecten
    ) {
        return informatieobjecten.stream().map(this::convertToREST).toList();
    }
}
