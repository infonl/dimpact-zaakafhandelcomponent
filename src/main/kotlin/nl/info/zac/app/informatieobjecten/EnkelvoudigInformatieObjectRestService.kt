/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.informatieobjecten.EnkelvoudigInformatieObjectDownloadService
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjecttypeConverter
import net.atos.zac.app.informatieobjecten.converter.RestZaakInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RESTDocumentVerplaatsGegevens
import net.atos.zac.app.informatieobjecten.model.RESTDocumentVerwijderenGegevens
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjectZoekParameters
import net.atos.zac.app.informatieobjecten.model.RestDocumentVerzendGegevens
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieObjectVersieGegevens
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject
import net.atos.zac.app.informatieobjecten.model.RestGekoppeldeZaakEnkelvoudigInformatieObject
import net.atos.zac.app.informatieobjecten.model.RestInformatieobjecttype
import net.atos.zac.app.informatieobjecten.model.RestZaakInformatieobject
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.util.MediaTypes
import net.atos.zac.webdav.WebdavHelper
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.extensions.isNuGeldig
import nl.info.zac.app.zaak.converter.RestGerelateerdeZaakConverter
import nl.info.zac.app.zaak.model.RelatieType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import java.io.IOException
import java.net.URI
import java.util.UUID
import nl.info.client.zgw.zrc.model.generated.Zaak

@Singleton
@Path("informatieobjecten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions", "LargeClass", "LongParameterList", "TooGenericExceptionThrown")
class EnkelvoudigInformatieObjectRestService @Inject constructor(
    private val drcClientService: DrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val zgwApiService: ZGWApiService,
    private val ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
    private val inboxDocumentenService: InboxDocumentenService,
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService,
    private val eventingService: EventingService,
    private val zaakInformatieobjectConverter: RestZaakInformatieobjectConverter,
    private val restInformatieobjectConverter: RestInformatieobjectConverter,
    private val restInformatieobjecttypeConverter: RestInformatieobjecttypeConverter,
    private val zaakHistoryLineConverter: ZaakHistoryLineConverter,
    private val restGerelateerdeZaakConverter: RestGerelateerdeZaakConverter,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val webdavHelper: WebdavHelper,
    private val policyService: PolicyService,
    private val enkelvoudigInformatieObjectDownloadService: EnkelvoudigInformatieObjectDownloadService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val enkelvoudigInformatieObjectConvertService: EnkelvoudigInformatieObjectConvertService,
) {

    @GET
    @Path("informatieobject/{uuid}")
    fun readEnkelvoudigInformatieobject(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("zaak") zaakUUID: UUID?
    ): RestEnkelvoudigInformatieobject =
        uuid
            .let(drcClientService::readEnkelvoudigInformatieobject)
            .let { enkelvoudigInformatieObject ->
                zaakUUID?.let(zrcClientService::readZaak).let {
                    restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, it)
                } ?: restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject)
            }

    @GET
    @Path("informatieobject/versie/{uuid}/{version}")
    fun readEnkelvoudigInformatieobjectVersion(
        @PathParam("uuid") uuid: UUID,
        @PathParam("version") version: Int
    ): RestEnkelvoudigInformatieobject =
        uuid
            .let(drcClientService::readEnkelvoudigInformatieobject)
            .let { currentVersion ->
                return when {
                    version < currentVersion.versie -> restInformatieobjectConverter.convertToREST(
                        drcClientService.readEnkelvoudigInformatieobjectVersie(uuid, version)
                    )
                    else -> restInformatieobjectConverter.convertToREST(currentVersion)
                }
            }

    @PUT
    @Path("informatieobjectenList")
    fun listEnkelvoudigInformatieobjecten(
        zoekParameters: RESTInformatieobjectZoekParameters
    ): List<RestEnkelvoudigInformatieobject> {
        val zaak = zoekParameters.zaakUUID?.let { zrcClientService.readZaak(it) }
        return zoekParameters.informatieobjectUUIDs?.let {
            restInformatieobjectConverter.convertUUIDsToREST(it, zaak)
        } ?: run {
            checkNotNull(zaak) { "Zoekparameters hebben geen waarde" }
            assertPolicy(policyService.readZaakRechten(zaak).lezen)
            var enkelvoudigInformatieobjectenVoorZaak = listEnkelvoudigInformatieobjectenVoorZaak(zaak)
            if (zoekParameters.gekoppeldeZaakDocumenten) {
                enkelvoudigInformatieobjectenVoorZaak.addAll(listGekoppeldeZaakInformatieObjectenVoorZaak(zaak))
            }
            zoekParameters.besluittypeUUID?.let { besluittypeUuid ->
                val besluittype = ztcClientService.readBesluittype(besluittypeUuid)
                val compareList = besluittype.informatieobjecttypen.map { it.extractUuid() }
                enkelvoudigInformatieobjectenVoorZaak = enkelvoudigInformatieobjectenVoorZaak.filter {
                    compareList.contains(it.informatieobjectTypeUUID)
                }.toMutableList()
            }
            enkelvoudigInformatieobjectenVoorZaak
        }
    }

    @GET
    @Path("informatieobjecten/zaak/{zaakUuid}/teVerzenden")
    fun listEnkelvoudigInformatieobjectenVoorVerzenden(
        @PathParam("zaakUuid") zaakUuid: UUID
    ): List<RestEnkelvoudigInformatieobject> {
        val zaak = zrcClientService.readZaak(zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).lezen)
        return zrcClientService.listZaakinformatieobjecten(zaak)
            .map { it.informatieobject }
            .map(drcClientService::readEnkelvoudigInformatieobject)
            .filter(::isVerzendenToegestaan)
            .map { restInformatieobjectConverter.convertToREST(it, zaak) }
    }

    @POST
    @Path("informatieobjecten/verzenden")
    fun sendDocument(restDocumentVerzendGegevens: RestDocumentVerzendGegevens) {
        val informatieobjecten = restDocumentVerzendGegevens.informatieobjecten
            .map(drcClientService::readEnkelvoudigInformatieobject)
        val zaak = zrcClientService.readZaak(restDocumentVerzendGegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)
        informatieobjecten.forEach { assertPolicy(isVerzendenToegestaan(it)) }
        informatieobjecten.forEach {
            enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                it.url.extractUuid(),
                restDocumentVerzendGegevens.verzenddatum,
                restDocumentVerzendGegevens.toelichting
            )
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("informatieobject/{zaakUuid}/{documentReferenceId}")
    fun createEnkelvoudigInformatieobjectAndUploadFile(
        @PathParam("zaakUuid") zaakUuid: UUID,
        @PathParam("documentReferenceId") documentReferenceId: String,
        @QueryParam("taakObject") isTaakObject: Boolean,
        @Valid @MultipartForm restEnkelvoudigInformatieobject: RestEnkelvoudigInformatieobject
    ): RestEnkelvoudigInformatieobject {
        val zaak = zrcClientService.readZaak(zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).toevoegenDocument)

        val enkelvoudigInformatieObjectCreateLockRequest = restEnkelvoudigInformatieobject.run(
            when {
                isTaakObject -> restInformatieobjectConverter::convertTaakObject
                else -> restInformatieobjectConverter::convertZaakObject
            }
        )
        val zaakInformatieobject = enkelvoudigInformatieObjectUpdateService.createZaakInformatieobjectForZaak(
            zaak = zaak,
            enkelvoudigInformatieObjectCreateLockRequest = enkelvoudigInformatieObjectCreateLockRequest,
            taskId = if (isTaakObject) documentReferenceId else null
        )

        return restInformatieobjectConverter.convertToREST(zaakInformatieobject)
    }

    @POST
    @Path("informatieobject/verplaats")
    fun verplaatsEnkelvoudigInformatieobject(documentVerplaatsGegevens: RESTDocumentVerplaatsGegevens) {
        val enkelvoudigInformatieobjectUUID = documentVerplaatsGegevens.documentUUID
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(
            enkelvoudigInformatieobjectUUID
        )
        val targetZaak = zrcClientService.readZaakByID(documentVerplaatsGegevens.nieuweZaakID)
        assertPolicy(
            policyService.readDocumentRechten(informatieobject, targetZaak).verplaatsen &&
                policyService.readZaakRechten(targetZaak).wijzigen
        )
        val toelichting = "Verplaatst: ${documentVerplaatsGegevens.bron} -> ${targetZaak.identificatie}"
        when {
            documentVerplaatsGegevens.vanuitOntkoppeldeDocumenten() -> ontkoppeldeDocumentenService.read(
                enkelvoudigInformatieobjectUUID
            ).let {
                zrcClientService.koppelInformatieobject(informatieobject, targetZaak, toelichting)
                ontkoppeldeDocumentenService.delete(it.id)
            }
            documentVerplaatsGegevens.vanuitInboxDocumenten() -> inboxDocumentenService.read(
                enkelvoudigInformatieobjectUUID
            ).let {
                zrcClientService.koppelInformatieobject(informatieobject, targetZaak, toelichting)
                inboxDocumentenService.delete(it.id)
            }
            else -> zrcClientService.readZaakByID(documentVerplaatsGegevens.bron).let {
                zrcClientService.verplaatsInformatieobject(informatieobject, it, targetZaak)
            }
        }
    }

    @GET
    @Path("informatieobjecttypes/{zaakTypeUuid}")
    fun listInformatieobjecttypes(@PathParam("zaakTypeUuid") zaakTypeID: UUID): List<RestInformatieobjecttype> =
        ztcClientService.readZaaktype(zaakTypeID).let {
            restInformatieobjecttypeConverter.convertFromUris(it.informatieobjecttypen)
        }

    @GET
    @Path("informatieobjecttypes/zaak/{zaakUuid}")
    fun listInformatieobjecttypesForZaak(@PathParam("zaakUuid") zaakUUID: UUID): List<RestInformatieobjecttype> =
        zrcClientService.readZaak(zaakUUID).zaaktype
            .let { ztcClientService.readZaaktype(it).informatieobjecttypen }
            .map(ztcClientService::readInformatieobjecttype)
            .filter { it.isNuGeldig() }
            .let(RestInformatieobjecttypeConverter::convert)

    @GET
    @Path("zaakinformatieobject/{uuid}/informatieobject")
    fun readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(
        @PathParam("uuid") uuid: UUID
    ): RestEnkelvoudigInformatieobject =
        zrcClientService.readZaakinformatieobject(uuid).informatieobject
            .let(drcClientService::readEnkelvoudigInformatieobject)
            .let(restInformatieobjectConverter::convertToREST)

    @GET
    @Path("informatieobject/{uuid}/zaakinformatieobjecten")
    fun listZaakInformatieobjecten(@PathParam("uuid") uuid: UUID): List<RestZaakInformatieobject> = uuid
        .let(drcClientService::readEnkelvoudigInformatieobject)
        .apply { assertPolicy(policyService.readDocumentRechten(this).lezen) }
        .let(zrcClientService::listZaakinformatieobjecten)
        .map(zaakInformatieobjectConverter::convert)

    @GET
    @Path("informatieobject/{uuid}/edit")
    fun editEnkelvoudigInformatieobjectInhoud(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("zaak") zaakUUID: UUID,
        @Context uriInfo: UriInfo
    ): Response {
        assertPolicy(
            policyService.readDocumentRechten(
                drcClientService.readEnkelvoudigInformatieobject(uuid),
                zrcClientService.readZaak(zaakUUID)
            ).wijzigen
        )
        return webdavHelper.createRedirectURL(uuid, uriInfo).let(Response::ok).build()
    }

    @DELETE
    @Path("/informatieobject/{uuid}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun deleteEnkelvoudigInformatieObject(
        @PathParam("uuid") uuid: UUID,
        documentVerwijderenGegevens: RESTDocumentVerwijderenGegevens
    ): Response {
        val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        val zaak = documentVerwijderenGegevens.zaakUuid?.let(zrcClientService::readZaak)
        assertPolicy(policyService.readDocumentRechten(enkelvoudigInformatieobject, zaak).verwijderen)
        zgwApiService.removeEnkelvoudigInformatieObjectFromZaak(
            enkelvoudigInformatieobject,
            documentVerwijderenGegevens.zaakUuid,
            documentVerwijderenGegevens.reden
        )

        // In geval van een ontkoppeld document
        if (documentVerwijderenGegevens.zaakUuid == null) {
            ontkoppeldeDocumentenService.delete(uuid)
        }
        return Response.noContent().build()
    }

    @GET
    @Path("/informatieobject/{uuid}/{version}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun readFileWithVersion(@PathParam("uuid") uuid: UUID, @PathParam("version") version: Int): Response =
        retrieveDocumentContent(uuid, version)

    @GET
    @Path("/informatieobject/{uuid}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun readFile(@PathParam("uuid") uuid: UUID): Response = retrieveDocumentContent(uuid, null)

    @GET
    @Path("/informatieobject/{uuid}/{versie}/preview")
    fun preview(@PathParam("uuid") uuid: UUID?, @PathParam("versie") versie: Int?): Response {
        val enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        assertPolicy(policyService.readDocumentRechten(enkelvoudigInformatieObject).lezen)
        return try {
            val inhoud = versie?.let {
                drcClientService.downloadEnkelvoudigInformatieobjectVersie(
                    uuid,
                    versie
                )
            } ?: drcClientService.downloadEnkelvoudigInformatieobject(uuid)
            Response.ok(inhoud)
                .header(
                    "Content-Disposition",
                    """inline; filename="${enkelvoudigInformatieObject.bestandsnaam}""""
                )
                .header("Content-Type", enkelvoudigInformatieObject.formaat).build()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @POST
    @Path("/download/zip")
    fun readFilesAsZip(uuids: List<String?>): Response {
        val informatieobjecten = uuids
            .map(UUID::fromString)
            .map(drcClientService::readEnkelvoudigInformatieobject)
        informatieobjecten
            .map(policyService::readDocumentRechten)
            .map { it.downloaden }
            .forEach { assertPolicy(it) }
        return informatieobjecten
            .let(enkelvoudigInformatieObjectDownloadService::getZipStreamOutput)
            .let(Response::ok)
            .header("Content-Type", MediaTypes.Application.ZIP.mediaType)
            .build()
    }

    @GET
    @Path("informatieobject/{uuid}/huidigeversie")
    fun readHuidigeVersieInformatieObject(
        @PathParam("uuid") uuid: UUID
    ): RestEnkelvoudigInformatieObjectVersieGegevens =
        drcClientService.readEnkelvoudigInformatieobject(uuid)
            .also { assertPolicy(policyService.readDocumentRechten(it).lezen) }
            .let(restInformatieobjectConverter::convertToRestEnkelvoudigInformatieObjectVersieGegevens)

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/informatieobject/update")
    fun updateEnkelvoudigInformatieobjectAndUploadFile(
        @Valid @MultipartForm enkelvoudigInformatieObjectVersieGegevens: RestEnkelvoudigInformatieObjectVersieGegevens
    ): RestEnkelvoudigInformatieobject {
        val document = drcClientService.readEnkelvoudigInformatieobject(
            enkelvoudigInformatieObjectVersieGegevens.uuid
        )
        assertPolicy(
            policyService.readDocumentRechten(
                document,
                zrcClientService.readZaak(enkelvoudigInformatieObjectVersieGegevens.zaakUuid)
            ).toevoegenNieuweVersie
        )
        val updatedDocument = restInformatieobjectConverter.convert(enkelvoudigInformatieObjectVersieGegevens)
        return updateEnkelvoudigInformatieobject(enkelvoudigInformatieObjectVersieGegevens, document, updatedDocument)
    }

    private fun updateEnkelvoudigInformatieobject(
        enkelvoudigInformatieObjectVersieGegevens: RestEnkelvoudigInformatieObjectVersieGegevens,
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest
    ): RestEnkelvoudigInformatieobject =
        enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
            enkelvoudigInformatieObject.url.extractUuid(),
            enkelvoudigInformatieObjectWithLockRequest,
            enkelvoudigInformatieObjectVersieGegevens.toelichting
        ).let(restInformatieobjectConverter::convertToREST)

    @POST
    @Path("/informatieobject/{uuid}/lock")
    fun lockDocument(@PathParam("uuid") uuid: UUID, @QueryParam("zaak") zaakUUID: UUID): Response {
        assertPolicy(
            drcClientService.readEnkelvoudigInformatieobject(uuid).let {
                !it.locked &&
                    policyService.readDocumentRechten(it, zrcClientService.readZaak(zaakUUID)).vergrendelen
            }
        )
        enkelvoudigInformatieObjectLockService.createLock(uuid, loggedInUserInstance.get().id)
        // Hiervoor wordt door open zaak geen notificatie verstuurd. Dus zelf het ScreenEvent versturen!
        eventingService.send(ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(uuid))
        return Response.ok().build()
    }

    @POST
    @Path("/informatieobject/{uuid}/unlock")
    fun unlockDocument(@PathParam("uuid") uuid: UUID, @QueryParam("zaak") zaakUUID: UUID?): Response {
        assertPolicy(
            drcClientService.readEnkelvoudigInformatieobject(uuid).let {
                it.locked &&
                    policyService.readDocumentRechten(it, zaakUUID?.let(zrcClientService::readZaak)).ontgrendelen
            }
        )

        enkelvoudigInformatieObjectLockService.deleteLock(uuid)
        // Hiervoor wordt door open zaak geen notificatie verstuurd. Dus zelf het ScreenEvent versturen!
        eventingService.send(ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(uuid))
        return Response.ok().build()
    }

    @GET
    @Path("informatieobject/{uuid}/historie")
    fun listHistorie(@PathParam("uuid") uuid: UUID?): List<HistoryLine> = uuid
        .apply {
            assertPolicy(
                policyService.readDocumentRechten(drcClientService.readEnkelvoudigInformatieobject(uuid)).lezen
            )
        }
        .let(drcClientService::listAuditTrail)
        .let(zaakHistoryLineConverter::convert)

    @GET
    @Path("informatieobject/{informatieObjectUuid}/zaakidentificaties")
    fun listZaakIdentificatiesForInformatieobject(
        @PathParam("informatieObjectUuid") informatieobjectUuid: UUID
    ): List<String> =
        drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
            .apply { assertPolicy(policyService.readDocumentRechten(this).lezen) }
            .let(zrcClientService::listZaakinformatieobjecten)
            .map { zrcClientService.readZaak(it.zaak).identificatie }

    @POST
    @Path("/informatieobject/{uuid}/onderteken")
    fun ondertekenInformatieObject(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("zaak") zaakUUID: UUID
    ): Response {
        val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        val zaak = zrcClientService.readZaak(zaakUUID)
        policyService.readDocumentRechten(enkelvoudigInformatieobject, zaak)
        val ondertekening = enkelvoudigInformatieobject.ondertekening
        val hasOndertekening = ondertekening?.datum != null
        assertPolicy(
            !hasOndertekening &&
                policyService.readDocumentRechten(enkelvoudigInformatieobject, zaak).ondertekenen
        )
        enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid)

        // Hiervoor wordt door open zaak geen notificatie verstuurd. Dus zelf het ScreenEvent versturen!
        eventingService.send(ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(enkelvoudigInformatieobject))

        return Response.ok().build()
    }

    @POST
    @Path("/informatieobject/{uuid}/convert")
    @Throws(IOException::class)
    fun convertInformatieObjectToPDF(
        @PathParam("uuid") enkelvoudigInformatieobjectUUID: UUID,
        @QueryParam("zaak") zaakUUID: UUID
    ): Response {
        val document = drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)
        assertPolicy(
            policyService.readDocumentRechten(document, zrcClientService.readZaak(zaakUUID)).wijzigen
        )
        enkelvoudigInformatieObjectConvertService.convertEnkelvoudigInformatieObject(
            document,
            enkelvoudigInformatieobjectUUID
        )
        return Response.ok().build()
    }

    private fun retrieveDocumentContent(uuid: UUID, version: Int?): Response {
        val enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        assertPolicy(policyService.readDocumentRechten(enkelvoudigInformatieObject).downloaden)
        return try {
            val documentContent = version?.let {
                drcClientService.downloadEnkelvoudigInformatieobjectVersie(uuid, version)
            } ?: drcClientService.downloadEnkelvoudigInformatieobject(uuid)
            Response.ok(documentContent)
                .header(
                    "Content-Disposition",
                    """attachment; filename="${enkelvoudigInformatieObject.bestandsnaam}""""
                )
                .build()
        } catch (ioException: IOException) {
            throw RuntimeException(ioException)
        }
    }

    private fun isVerzendenToegestaan(informatieobject: EnkelvoudigInformatieObject): Boolean =
        informatieobject.vertrouwelijkheidaanduiding.let {
            informatieobject.status == StatusEnum.DEFINITIEF &&
                it != VertrouwelijkheidaanduidingEnum.CONFIDENTIEEL &&
                it != VertrouwelijkheidaanduidingEnum.GEHEIM &&
                it != VertrouwelijkheidaanduidingEnum.ZEER_GEHEIM &&
                informatieobject.ontvangstdatum == null &&
                informatieobject.formaat == MediaTypes.Application.PDF.mediaType
        }

    private fun listEnkelvoudigInformatieobjectenVoorZaak(zaak: Zaak): MutableList<RestEnkelvoudigInformatieobject> =
        zaak.let(zrcClientService::listZaakinformatieobjecten)
            .map(restInformatieobjectConverter::convertToREST)
            .toMutableList()

    private fun listGekoppeldeZaakEnkelvoudigInformatieobjectenVoorZaak(
        zaakURI: URI,
        relatieType: RelatieType
    ): List<RestGekoppeldeZaakEnkelvoudigInformatieObject> =
        zaakURI.let(zrcClientService::readZaak)
            .let { zaak ->
                zrcClientService.listZaakinformatieobjecten(zaak)
                    .map { restInformatieobjectConverter.convertToREST(it, relatieType, zaak) }
            }

    private fun listGekoppeldeZaakInformatieObjectenVoorZaak(
        zaak: Zaak
    ): List<RestGekoppeldeZaakEnkelvoudigInformatieObject> =
        mutableListOf<RestGekoppeldeZaakEnkelvoudigInformatieObject>().apply {
            zaak.deelzaken?.forEach {
                addAll(listGekoppeldeZaakEnkelvoudigInformatieobjectenVoorZaak(it, RelatieType.DEELZAAK))
            }
            zaak.hoofdzaak?.let {
                addAll(listGekoppeldeZaakEnkelvoudigInformatieobjectenVoorZaak(it, RelatieType.HOOFDZAAK))
            }
            zaak.relevanteAndereZaken?.forEach {
                addAll(
                    listGekoppeldeZaakEnkelvoudigInformatieobjectenVoorZaak(
                        it.url,
                        restGerelateerdeZaakConverter.convertToRelatieType(it.aardRelatie)
                    )
                )
            }
        }
}
