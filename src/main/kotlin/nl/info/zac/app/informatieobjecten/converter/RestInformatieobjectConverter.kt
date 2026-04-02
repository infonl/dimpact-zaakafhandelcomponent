/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.client.zgw.shared.exception.ZgwErrorException
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.configuration.model.toRestTaal
import nl.info.zac.app.identity.model.toRestUser
import nl.info.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieObjectVersieGegevens
import nl.info.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject
import nl.info.zac.app.informatieobjecten.model.RestFileUpload
import nl.info.zac.app.informatieobjecten.model.RestGekoppeldeZaakEnkelvoudigInformatieObject
import nl.info.zac.app.informatieobjecten.model.toRestOndertekening
import nl.info.zac.app.policy.model.toRestDocumentRechten
import nl.info.zac.app.task.model.RestTaskDocumentData
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.policy.PolicyService
import nl.info.zac.util.toBase64String
import org.eclipse.jetty.http.HttpStatus
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

@Suppress("LongParameterList", "TooManyFunctions")
class RestInformatieobjectConverter @Inject constructor(
    private val brcClientService: BrcClientService,
    private val configurationService: ConfigurationService,
    private val drcClientService: DrcClientService,
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService,
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService
) {
    companion object {
        private val LOG = Logger.getLogger(RestInformatieobjectConverter::class.java.name)
    }

    fun convertToREST(zaakInformatieobjecten: List<ZaakInformatieobject>): List<RestEnkelvoudigInformatieobject> =
        zaakInformatieobjecten.map(::convertToREST)

    fun convertToREST(zaakInformatieObject: ZaakInformatieobject): RestEnkelvoudigInformatieobject {
        val enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
            zaakInformatieObject.informatieobject
        )
        val zaak = zrcClientService.readZaak(zaakInformatieObject.zaakUUID)
        return convertToREST(enkelvoudigInformatieObject = enkelvoudigInformatieObject, zaak = zaak)
    }

    fun convertToREST(enkelvoudigInformatieObject: EnkelvoudigInformatieObject): RestEnkelvoudigInformatieobject =
        convertToREST(enkelvoudigInformatieObject = enkelvoudigInformatieObject, zaak = null)

    fun convertToREST(enkelvoudigInformatieObject: EnkelvoudigInformatieObject, zaak: Zaak?): RestEnkelvoudigInformatieobject {
        val enkelvoudigInformatieObjectUUID = enkelvoudigInformatieObject.url.extractUuid()
        val lock = if (enkelvoudigInformatieObject.locked) {
            enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID)
        } else {
            null
        }
        val documentRechten = policyService.readDocumentRechten(enkelvoudigInformatieObject, lock, zaak)
        val isBesluitDocument = brcClientService.isInformatieObjectGekoppeldAanBesluit(
            enkelvoudigInformatieObject.url
        )
        val restEnkelvoudigInformatieobject = RestEnkelvoudigInformatieobject(
            uuid = enkelvoudigInformatieObjectUUID,
            identificatie = enkelvoudigInformatieObject.identificatie,
            rechten = documentRechten.toRestDocumentRechten(),
            isBesluitDocument = isBesluitDocument
        )
        if (documentRechten.lezen) {
            convertEnkelvoudigInformatieObject(
                enkelvoudigInformatieObject = enkelvoudigInformatieObject,
                lock = lock,
                restEnkelvoudigInformatieobject = restEnkelvoudigInformatieobject
            )
            val ondertekening = enkelvoudigInformatieObject.ondertekening
            if (ondertekening != null && ondertekening.soort != null && ondertekening.datum != null) {
                restEnkelvoudigInformatieobject.ondertekening = ondertekening.toRestOndertekening()
            }
        } else {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.identificatie
        }
        return restEnkelvoudigInformatieobject
    }

    private fun convertEnkelvoudigInformatieObject(
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        lock: EnkelvoudigInformatieObjectLock?,
        restEnkelvoudigInformatieobject: RestEnkelvoudigInformatieobject
    ) {
        restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.titel
        if (enkelvoudigInformatieObject.bronorganisatie != null) {
            restEnkelvoudigInformatieobject.bronorganisatie =
                if (enkelvoudigInformatieObject.bronorganisatie == configurationService.readBronOrganisatie()) {
                    null
                } else {
                    enkelvoudigInformatieObject.bronorganisatie
                }
        }
        restEnkelvoudigInformatieobject.creatiedatum = enkelvoudigInformatieObject.creatiedatum
        if (enkelvoudigInformatieObject.vertrouwelijkheidaanduiding != null) {
            // we use the uppercase version of this enum in the ZAC backend API
            restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding =
                enkelvoudigInformatieObject.vertrouwelijkheidaanduiding.name
        }
        restEnkelvoudigInformatieobject.auteur = enkelvoudigInformatieObject.auteur
        if (enkelvoudigInformatieObject.status != null) {
            restEnkelvoudigInformatieobject.status = enkelvoudigInformatieObject.status
        }
        restEnkelvoudigInformatieobject.formaat = enkelvoudigInformatieObject.formaat

        val taal = enkelvoudigInformatieObject.taal?.let { configurationService.findTaal(it) }
        if (taal != null) {
            restEnkelvoudigInformatieobject.taal = taal.naam
        }

        restEnkelvoudigInformatieobject.versie = enkelvoudigInformatieObject.versie
        restEnkelvoudigInformatieobject.registratiedatumTijd = enkelvoudigInformatieObject.beginRegistratie.toZonedDateTime()
        restEnkelvoudigInformatieobject.bestandsnaam = enkelvoudigInformatieObject.bestandsnaam
        if (enkelvoudigInformatieObject.link != null) {
            restEnkelvoudigInformatieobject.link = enkelvoudigInformatieObject.link.toString()
        }
        restEnkelvoudigInformatieobject.beschrijving = enkelvoudigInformatieObject.beschrijving
        restEnkelvoudigInformatieobject.ontvangstdatum = enkelvoudigInformatieObject.ontvangstdatum
        restEnkelvoudigInformatieobject.verzenddatum = enkelvoudigInformatieObject.verzenddatum
        if (lock != null) {
            restEnkelvoudigInformatieobject.gelockedDoor = identityService.readUser(lock.userId!!).toRestUser()
        }
        restEnkelvoudigInformatieobject.bestandsomvang = enkelvoudigInformatieObject.bestandsomvang?.toLong() ?: 0
        restEnkelvoudigInformatieobject.informatieobjectTypeOmschrijving = ztcClientService
            .readInformatieobjecttype(enkelvoudigInformatieObject.informatieobjecttype).omschrijving
        restEnkelvoudigInformatieobject.informatieobjectTypeUUID = enkelvoudigInformatieObject.informatieobjecttype.extractUuid()
    }

    fun convertEnkelvoudigInformatieObject(
        restEnkelvoudigInformatieobject: RestEnkelvoudigInformatieobject
    ): EnkelvoudigInformatieObjectCreateLockRequest = buildEnkelvoudigInformatieObjectData(
        restEnkelvoudigInformatieobject
    ).apply {
        inhoud = restEnkelvoudigInformatieobject.file!!.toBase64String()
        bestandsomvang = restEnkelvoudigInformatieobject.file!!.size
        formaat = restEnkelvoudigInformatieobject.formaat
    }

    private fun buildEnkelvoudigInformatieObjectData(
        restEnkelvoudigInformatieobject: RestEnkelvoudigInformatieobject
    ): EnkelvoudigInformatieObjectCreateLockRequest {
        val informatieObjectType = restEnkelvoudigInformatieobject.informatieobjectTypeUUID?.let {
            ztcClientService.readInformatieobjecttype(it)
        }
        return EnkelvoudigInformatieObjectCreateLockRequest().apply {
            bronorganisatie = configurationService.readBronOrganisatie()
            creatiedatum = restEnkelvoudigInformatieobject.creatiedatum
            titel = restEnkelvoudigInformatieobject.titel
            auteur = restEnkelvoudigInformatieobject.auteur
            taal = restEnkelvoudigInformatieobject.taal
            informatieobjecttype = informatieObjectType?.url
            bestandsnaam = restEnkelvoudigInformatieobject.bestandsnaam
            beschrijving = restEnkelvoudigInformatieobject.beschrijving
            status = restEnkelvoudigInformatieobject.status
            verzenddatum = restEnkelvoudigInformatieobject.verzenddatum
            ontvangstdatum = restEnkelvoudigInformatieobject.ontvangstdatum
            restEnkelvoudigInformatieobject.vertrouwelijkheidaanduiding?.let { v ->
                vertrouwelijkheidaanduiding =
                    VertrouwelijkheidaanduidingEnum.valueOf(
                        // the values of the enums generated by OpenAPI Generator are the
                        // uppercase variants of the strings used in the APIs
                        v.uppercase()
                    )
            }
        }
    }

    fun convert(documentData: RestTaskDocumentData, bestand: RestFileUpload): EnkelvoudigInformatieObjectCreateLockRequest {
        val informatieObjectType = ztcClientService.readInformatieobjecttype(documentData.documentType.uuid)
        return EnkelvoudigInformatieObjectCreateLockRequest().apply {
            bronorganisatie = configurationService.readBronOrganisatie()
            creatiedatum = LocalDate.now()
            titel = documentData.documentTitel
            auteur = loggedInUserInstance.get().getFullName()
            taal = ConfigurationService.TAAL_NEDERLANDS
            informatieobjecttype = informatieObjectType.url
            inhoud = bestand.file!!.toBase64String()
            formaat = bestand.type
            bestandsnaam = bestand.filename
            status = StatusEnum.DEFINITIEF
            vertrouwelijkheidaanduiding =
                VertrouwelijkheidaanduidingEnum.valueOf(documentData.documentType.vertrouwelijkheidaanduiding!!)
        }
    }

    fun convertToRestEnkelvoudigInformatieObjectVersieGegevens(
        informatieobject: EnkelvoudigInformatieObject
    ): RestEnkelvoudigInformatieObjectVersieGegevens {
        val restEnkelvoudigInformatieObjectVersieGegevens = RestEnkelvoudigInformatieObjectVersieGegevens()
        restEnkelvoudigInformatieObjectVersieGegevens.uuid = informatieobject.url.extractUuid()
        if (informatieobject.status != null) {
            restEnkelvoudigInformatieObjectVersieGegevens.status = informatieobject.status
        }
        if (informatieobject.vertrouwelijkheidaanduiding != null) {
            // we use the uppercase version of this enum in the ZAC backend API
            restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding =
                informatieobject.vertrouwelijkheidaanduiding.name
        }
        restEnkelvoudigInformatieObjectVersieGegevens.beschrijving = informatieobject.beschrijving
        restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum = informatieobject.verzenddatum
        restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum = informatieobject.ontvangstdatum
        restEnkelvoudigInformatieObjectVersieGegevens.titel = informatieobject.titel
        restEnkelvoudigInformatieObjectVersieGegevens.auteur = informatieobject.auteur
        val taal = configurationService.findTaal(informatieobject.taal)
        if (taal != null) {
            restEnkelvoudigInformatieObjectVersieGegevens.taal = taal.toRestTaal()
        }
        restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam = informatieobject.bestandsnaam
        restEnkelvoudigInformatieObjectVersieGegevens.informatieobjectTypeUUID = informatieobject.informatieobjecttype.extractUuid()
        return restEnkelvoudigInformatieObjectVersieGegevens
    }

    @Suppress("MaxLineLength")
    fun convert(
        restEnkelvoudigInformatieObjectVersieGegevens: RestEnkelvoudigInformatieObjectVersieGegevens
    ): EnkelvoudigInformatieObjectWithLockRequest {
        val enkelvoudigInformatieObjectWithLockRequest = createEnkelvoudigInformatieObjectWithLockData(
            restEnkelvoudigInformatieObjectVersieGegevens
        )
        if (hasFileContent(restEnkelvoudigInformatieObjectVersieGegevens)) {
            enkelvoudigInformatieObjectWithLockRequest.inhoud = restEnkelvoudigInformatieObjectVersieGegevens.file!!.toBase64String()
            enkelvoudigInformatieObjectWithLockRequest.bestandsnaam = restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam
            enkelvoudigInformatieObjectWithLockRequest.bestandsomvang = restEnkelvoudigInformatieObjectVersieGegevens.file!!.size
            enkelvoudigInformatieObjectWithLockRequest.formaat = restEnkelvoudigInformatieObjectVersieGegevens.formaat
        }
        enkelvoudigInformatieObjectWithLockRequest.informatieobjecttype =
            ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieObjectVersieGegevens.informatieobjectTypeUUID!!).url
        return enkelvoudigInformatieObjectWithLockRequest
    }

    private fun createEnkelvoudigInformatieObjectWithLockData(
        restEnkelvoudigInformatieObjectVersieGegevens: RestEnkelvoudigInformatieObjectVersieGegevens
    ): EnkelvoudigInformatieObjectWithLockRequest {
        val enkelvoudigInformatieObjectWithLockData = EnkelvoudigInformatieObjectWithLockRequest()
        if (restEnkelvoudigInformatieObjectVersieGegevens.status != null) {
            enkelvoudigInformatieObjectWithLockData.status = restEnkelvoudigInformatieObjectVersieGegevens.status
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding != null) {
            enkelvoudigInformatieObjectWithLockData.vertrouwelijkheidaanduiding =
                // convert this enum to uppercase in case the client sends it in lowercase
                VertrouwelijkheidaanduidingEnum.valueOf(
                    restEnkelvoudigInformatieObjectVersieGegevens.vertrouwelijkheidaanduiding!!.uppercase()
                )
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.beschrijving != null) {
            enkelvoudigInformatieObjectWithLockData.beschrijving = restEnkelvoudigInformatieObjectVersieGegevens.beschrijving
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum != null) {
            enkelvoudigInformatieObjectWithLockData.verzenddatum = restEnkelvoudigInformatieObjectVersieGegevens.verzenddatum
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum != null) {
            enkelvoudigInformatieObjectWithLockData.ontvangstdatum = restEnkelvoudigInformatieObjectVersieGegevens.ontvangstdatum
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.titel != null) {
            enkelvoudigInformatieObjectWithLockData.titel = restEnkelvoudigInformatieObjectVersieGegevens.titel
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.taal != null) {
            enkelvoudigInformatieObjectWithLockData.taal = restEnkelvoudigInformatieObjectVersieGegevens.taal!!.code
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.auteur != null) {
            enkelvoudigInformatieObjectWithLockData.auteur = restEnkelvoudigInformatieObjectVersieGegevens.auteur
        }
        if (restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam != null) {
            enkelvoudigInformatieObjectWithLockData.bestandsnaam = restEnkelvoudigInformatieObjectVersieGegevens.bestandsnaam
        }
        return enkelvoudigInformatieObjectWithLockData
    }

    fun convertUUIDsToREST(enkelvoudigInformatieobjectUUIDs: List<UUID>, zaak: Zaak?): List<RestEnkelvoudigInformatieobject> =
        enkelvoudigInformatieobjectUUIDs.mapNotNull { enkelvoudigInformatieobjectUUID ->
            try {
                convertToREST(
                    enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
                        enkelvoudigInformatieobjectUUID
                    ),
                    zaak = zaak
                )
            } catch (zgwErrorException: ZgwErrorException) {
                if (zgwErrorException.zgwError.status != HttpStatus.NOT_FOUND_404) {
                    throw zgwErrorException
                }
                LOG.severe { "Document niet gevonden: $enkelvoudigInformatieobjectUUID" }
                null
            }
        }

    fun convertToREST(
        zaakInformatieObject: ZaakInformatieobject,
        relatieType: RelatieType,
        zaak: Zaak
    ): RestGekoppeldeZaakEnkelvoudigInformatieObject {
        val enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(
            zaakInformatieObject.informatieobject
        )
        val enkelvoudigInformatieObjectUUID = enkelvoudigInformatieObject.url.extractUuid()
        val lock = if (enkelvoudigInformatieObject.locked) {
            enkelvoudigInformatieObjectLockService.findLock(enkelvoudigInformatieObjectUUID)
        } else {
            null
        }
        val documentRechten = policyService.readDocumentRechten(enkelvoudigInformatieObject, lock, zaak)
        val restEnkelvoudigInformatieobject = RestGekoppeldeZaakEnkelvoudigInformatieObject()
        restEnkelvoudigInformatieobject.uuid = enkelvoudigInformatieObjectUUID
        restEnkelvoudigInformatieobject.identificatie = enkelvoudigInformatieObject.identificatie
        restEnkelvoudigInformatieobject.rechten = documentRechten.toRestDocumentRechten()
        if (documentRechten.lezen) {
            convertEnkelvoudigInformatieObject(
                enkelvoudigInformatieObject = enkelvoudigInformatieObject,
                lock = lock,
                restEnkelvoudigInformatieobject = restEnkelvoudigInformatieobject
            )
            restEnkelvoudigInformatieobject.relatieType = relatieType
            restEnkelvoudigInformatieobject.zaakIdentificatie = zaak.identificatie
            restEnkelvoudigInformatieobject.zaakUUID = zaak.uuid
        } else {
            restEnkelvoudigInformatieobject.titel = enkelvoudigInformatieObject.identificatie
        }
        return restEnkelvoudigInformatieobject
    }

    fun convertInformatieobjectenToREST(informatieobjecten: List<EnkelvoudigInformatieObject>): List<RestEnkelvoudigInformatieobject> =
        informatieobjecten.map { convertToREST(it) }

    private fun hasFileContent(versieGegevens: RestEnkelvoudigInformatieObjectVersieGegevens) =
        versieGegevens.file != null &&
            versieGegevens.file!!.isNotEmpty() &&
            versieGegevens.bestandsnaam != null &&
            versieGegevens.formaat != null
}
