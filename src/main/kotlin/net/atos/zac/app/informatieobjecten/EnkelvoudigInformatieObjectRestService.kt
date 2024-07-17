/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten

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
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import net.atos.client.officeconverter.OfficeConverterClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.DrcClientUtil.convertByteArrayToBase64String
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.util.InformatieObjectTypeUtil.isNuGeldig
import net.atos.zac.app.audit.converter.RESTHistorieRegelConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.converter.RESTInformatieobjecttypeConverter
import net.atos.zac.app.informatieobjecten.converter.RESTZaakInformatieobjectConverter
import net.atos.zac.app.informatieobjecten.model.RESTDocumentCreatieGegevens
import net.atos.zac.app.informatieobjecten.model.RESTDocumentCreatieResponse
import net.atos.zac.app.informatieobjecten.model.RESTDocumentVerplaatsGegevens
import net.atos.zac.app.informatieobjecten.model.RESTDocumentVerwijderenGegevens
import net.atos.zac.app.informatieobjecten.model.RESTDocumentVerzendGegevens
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieObjectVersieGegevens
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieobject
import net.atos.zac.app.informatieobjecten.model.RESTGekoppeldeZaakEnkelvoudigInformatieObject
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjectZoekParameters
import net.atos.zac.app.informatieobjecten.model.RESTInformatieobjecttype
import net.atos.zac.app.informatieobjecten.model.RESTZaakInformatieobject
import net.atos.zac.app.zaak.converter.RESTGerelateerdeZaakConverter
import net.atos.zac.app.zaak.model.RelatieType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.documentcreatie.model.DocumentCreatieGegevens
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.documenten.OntkoppeldeDocumentenService
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.FlowableTaskService
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.TaakVariabelenService.readTaskdocuments
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.PolicyService.assertPolicy
import net.atos.zac.util.UriUtil
import net.atos.zac.util.UriUtil.uuidFromURI
import net.atos.zac.webdav.WebdavHelper
import net.atos.zac.websocket.event.ScreenEventType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import java.io.IOException
import java.net.URI
import java.util.UUID

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
    private val zrcClientService: ZRCClientService,
    private val zgwApiService: ZGWApiService,
    private val flowableTaskService: FlowableTaskService,
    private val taakVariabelenService: TaakVariabelenService,
    private val ontkoppeldeDocumentenService: OntkoppeldeDocumentenService,
    private val inboxDocumentenService: InboxDocumentenService,
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService,
    private val eventingService: EventingService,
    private val zaakInformatieobjectConverter: RESTZaakInformatieobjectConverter,
    private val informatieobjectConverter: RESTInformatieobjectConverter,
    private val informatieObjecttypeConverter: RESTInformatieobjecttypeConverter,
    private val historieRegelConverter: RESTHistorieRegelConverter,
    private val gerelateerdeZaakConverter: RESTGerelateerdeZaakConverter,
    private val documentCreatieService: DocumentCreatieService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val webdavHelper: WebdavHelper,
    private val policyService: PolicyService,
    private val enkelvoudigInformatieObjectDownloadService: EnkelvoudigInformatieObjectDownloadService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val officeConverterClientService: OfficeConverterClientService
) {
    companion object {
        private const val MEDIA_TYPE_PDF = "application/pdf"
        private const val TOELICHTING_PDF = "Geconverteerd naar PDF"
    }

    @GET
    @Path("informatieobject/{uuid}")
    fun readEnkelvoudigInformatieobject(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("zaak") zaakUUID: UUID?
    ): RESTEnkelvoudigInformatieobject =
        uuid
            .let(drcClientService::readEnkelvoudigInformatieobject)
            .let { enkelvoudigInformatieObject ->
                zaakUUID?.let(zrcClientService::readZaak).let {
                    informatieobjectConverter.convertToREST(enkelvoudigInformatieObject, it)
                } ?: informatieobjectConverter.convertToREST(enkelvoudigInformatieObject)
            }

    @GET
    @Path("informatieobject/versie/{uuid}/{version}")
    fun readEnkelvoudigInformatieobject(
        @PathParam("uuid") uuid: UUID,
        @PathParam("version") version: Int
    ): RESTEnkelvoudigInformatieobject =
        uuid
            .let(drcClientService::readEnkelvoudigInformatieobject)
            .let { currentVersion ->
                return when {
                    version < currentVersion.versie -> informatieobjectConverter.convertToREST(
                        drcClientService.readEnkelvoudigInformatieobjectVersie(uuid, version)
                    )
                    else -> informatieobjectConverter.convertToREST(currentVersion)
                }
            }

    @PUT
    @Path("informatieobjectenList")
    fun listEnkelvoudigInformatieobjecten(
        zoekParameters: RESTInformatieobjectZoekParameters
    ): List<RESTEnkelvoudigInformatieobject> {
        val zaak = zoekParameters.zaakUUID?.let { zrcClientService.readZaak(it) }
        zoekParameters.informatieobjectUUIDs?.let {
            return informatieobjectConverter.convertUUIDsToREST(it, zaak)
        } ?: run {
            checkNotNull(zaak) { "Zoekparameters hebben geen waarde" }
            assertPolicy(policyService.readZaakRechten(zaak).lezen)
            var enkelvoudigInformatieobjectenVoorZaak = listEnkelvoudigInformatieobjectenVoorZaak(zaak)
            if (zoekParameters.gekoppeldeZaakDocumenten) {
                enkelvoudigInformatieobjectenVoorZaak.addAll(listGekoppeldeZaakInformatieObjectenVoorZaak(zaak))
            }
            zoekParameters.besluittypeUUID?.let { besluittypeUuid ->
                val besluittype = ztcClientService.readBesluittype(besluittypeUuid)
                val compareList = besluittype.informatieobjecttypen.map { UriUtil.uuidFromURI(it) }.toList()
                enkelvoudigInformatieobjectenVoorZaak = enkelvoudigInformatieobjectenVoorZaak.filter {
                    compareList.contains(it.informatieobjectTypeUUID)
                }.toMutableList()
            }
            return enkelvoudigInformatieobjectenVoorZaak
        }
    }

    @GET
    @Path("informatieobjecten/zaak/{zaakUuid}/teVerzenden")
    fun listEnkelvoudigInformatieobjectenVoorVerzenden(
        @PathParam("zaakUuid") zaakUuid: UUID
    ): List<RESTEnkelvoudigInformatieobject> {
        val zaak = zrcClientService.readZaak(zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).lezen)
        return zrcClientService.listZaakinformatieobjecten(zaak)
            .map { it.informatieobject }
            .map(drcClientService::readEnkelvoudigInformatieobject)
            .filter(::isVerzendenToegestaan)
            .map { informatieobjectConverter.convertToREST(it, zaak) }
            .toList()
    }

    @POST
    @Path("informatieobjecten/verzenden")
    fun verzenden(gegevens: RESTDocumentVerzendGegevens) {
        val informatieobjecten = gegevens.informatieobjecten
            .map(drcClientService::readEnkelvoudigInformatieobject)
            .toList()
        val zaak = zrcClientService.readZaak(gegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).wijzigen)
        informatieobjecten.forEach {
            assertPolicy(isVerzendenToegestaan(it))
        }
        informatieobjecten.forEach {
            enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                URIUtil.parseUUIDFromResourceURI(it.url),
                gegevens.verzenddatum,
                gegevens.toelichting
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
        @Valid @MultipartForm restEnkelvoudigInformatieobject: RESTEnkelvoudigInformatieobject
    ): RESTEnkelvoudigInformatieobject {
        val zaak = zrcClientService.readZaak(zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak).toevoegenDocument)

        val enkelvoudigInformatieObjectCreateLockRequest = restEnkelvoudigInformatieobject.run(
            when {
                isTaakObject -> informatieobjectConverter::convertTaakObject
                else -> informatieobjectConverter::convertZaakObject
            }
        )
        val zaakInformatieobject = zgwApiService.createZaakInformatieobjectForZaak(
            zaak,
            enkelvoudigInformatieObjectCreateLockRequest,
            enkelvoudigInformatieObjectCreateLockRequest.titel,
            enkelvoudigInformatieObjectCreateLockRequest.beschrijving,
            ConfiguratieService.OMSCHRIJVING_VOORWAARDEN_GEBRUIKSRECHTEN
        )

        if (isTaakObject) {
            addZaakInformatieobjectToTaak(zaakInformatieobject, documentReferenceId)
        }
        return informatieobjectConverter.convertToREST(zaakInformatieobject)
    }

    private fun addZaakInformatieobjectToTaak(
        zaakInformatieobject: ZaakInformatieobject,
        documentReferentieId: String
    ) {
        val task = flowableTaskService.findOpenTask(documentReferentieId)
            ?: throw WebApplicationException(
                "No open task found with task id: '$documentReferentieId'",
                Response.Status.CONFLICT
            )
        assertPolicy(policyService.readTaakRechten(task).toevoegenDocument)

        mutableListOf<UUID>().let {
            it.addAll(readTaskdocuments(task))
            it.add(uuidFromURI(zaakInformatieobject.informatieobject))
            taakVariabelenService.setTaakdocumenten(task, it)
        }
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
    fun listInformatieobjecttypes(@PathParam("zaakTypeUuid") zaakTypeID: UUID): List<RESTInformatieobjecttype> =
        ztcClientService.readZaaktype(zaakTypeID).let {
            informatieObjecttypeConverter.convertFromUris(it.informatieobjecttypen)
        }

    @GET
    @Path("informatieobjecttypes/zaak/{zaakUuid}")
    fun listInformatieobjecttypesForZaak(@PathParam("zaakUuid") zaakID: UUID): List<RESTInformatieobjecttype> =
        zrcClientService.readZaak(zaakID).zaaktype
            .let { ztcClientService.readZaaktype(it).informatieobjecttypen }
            .map { ztcClientService.readInformatieobjecttype(it) }
            .filter { isNuGeldig(it) }
            .toList()
            .let(informatieObjecttypeConverter::convert)

    @GET
    @Path("zaakinformatieobject/{uuid}/informatieobject")
    fun readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(
        @PathParam("uuid") uuid: UUID
    ): RESTEnkelvoudigInformatieobject =
        zrcClientService.readZaakinformatieobject(uuid).informatieobject
            .let(drcClientService::readEnkelvoudigInformatieobject)
            .let(informatieobjectConverter::convertToREST)

    @GET
    @Path("informatieobject/{uuid}/zaakinformatieobjecten")
    fun listZaakInformatieobjecten(@PathParam("uuid") uuid: UUID): List<RESTZaakInformatieobject> = uuid
        .let(drcClientService::readEnkelvoudigInformatieobject)
        .apply { assertPolicy(policyService.readDocumentRechten(this).lezen) }
        .let(zrcClientService::listZaakinformatieobjecten)
        .map(zaakInformatieobjectConverter::convert)
        .toList()

    @GET
    @Path("informatieobject/{uuid}/edit")
    fun editEnkelvoudigInformatieobjectInhoud(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("zaak") zaakUUID: UUID?,
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

    @GET
    @Path("/informatieobject/{uuid}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun readFile(@PathParam("uuid") uuid: UUID): Response = readFile(uuid, null)

    @DELETE
    @Path("/informatieobject/{uuid}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun deleteEnkelvoudigInformatieObject(
        @PathParam("uuid") uuid: UUID?,
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
    fun readFile(@PathParam("uuid") uuid: UUID?, @PathParam("version") version: Int?): Response {
        val enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        assertPolicy(policyService.readDocumentRechten(enkelvoudigInformatieObject).downloaden)
        try {
            val inhoud = version?.let {
                drcClientService.downloadEnkelvoudigInformatieobjectVersie(uuid, it)
            } ?: drcClientService.downloadEnkelvoudigInformatieobject(uuid)
            return Response.ok(inhoud)
                .header(
                    "Content-Disposition",
                    """attachment; filename="${enkelvoudigInformatieObject.bestandsnaam}""""
                )
                .build()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @GET
    @Path("/informatieobject/{uuid}/{versie}/preview")
    fun preview(@PathParam("uuid") uuid: UUID?, @PathParam("versie") versie: Int?): Response {
        val enkelvoudigInformatieObject = drcClientService.readEnkelvoudigInformatieobject(uuid)
        assertPolicy(policyService.readDocumentRechten(enkelvoudigInformatieObject).lezen)
        try {
            val inhoud = versie?.let {
                drcClientService.downloadEnkelvoudigInformatieobjectVersie(
                    uuid,
                    versie
                )
            } ?: drcClientService.downloadEnkelvoudigInformatieobject(uuid)
            return Response.ok(inhoud)
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
            .toList()

        informatieobjecten
            .map(policyService::readDocumentRechten)
            .map { it.downloaden }
            .forEach(PolicyService::assertPolicy)

        return informatieobjecten
            .let(enkelvoudigInformatieObjectDownloadService::getZipStreamOutput)
            .let(Response::ok)
            .header("Content-Type", "application/zip")
            .build()
    }

    @GET
    @Path("informatieobject/{uuid}/huidigeversie")
    fun readHuidigeVersieInformatieObject(
        @PathParam("uuid") uuid: UUID
    ): RESTEnkelvoudigInformatieObjectVersieGegevens = drcClientService.readEnkelvoudigInformatieobject(
        uuid
    ).let {
        assertPolicy(policyService.readDocumentRechten(it).lezen)
        return informatieobjectConverter.convertToRESTEnkelvoudigInformatieObjectVersieGegevens(it)
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/informatieobject/update")
    fun updateEnkelvoudigInformatieobjectAndUploadFile(
        @Valid @MultipartForm enkelvoudigInformatieObjectVersieGegevens: RESTEnkelvoudigInformatieObjectVersieGegevens
    ): RESTEnkelvoudigInformatieobject {
        val document = drcClientService.readEnkelvoudigInformatieobject(
            enkelvoudigInformatieObjectVersieGegevens.uuid
        )
        assertPolicy(
            policyService.readDocumentRechten(
                document,
                zrcClientService.readZaak(enkelvoudigInformatieObjectVersieGegevens.zaakUuid)
            ).toevoegenNieuweVersie
        )
        val updatedDocument = informatieobjectConverter.convert(enkelvoudigInformatieObjectVersieGegevens)
        return updateEnkelvoudigInformatieobject(enkelvoudigInformatieObjectVersieGegevens, document, updatedDocument)
    }

    private fun updateEnkelvoudigInformatieobject(
        enkelvoudigInformatieObjectVersieGegevens: RESTEnkelvoudigInformatieObjectVersieGegevens,
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest
    ): RESTEnkelvoudigInformatieobject =
        enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
            URIUtil.parseUUIDFromResourceURI(enkelvoudigInformatieObject.url),
            enkelvoudigInformatieObjectWithLockRequest,
            enkelvoudigInformatieObjectVersieGegevens.toelichting
        )
            .let(informatieobjectConverter::convertToREST)

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
    fun listHistorie(@PathParam("uuid") uuid: UUID?): List<RESTHistorieRegel> = uuid
        .apply {
            assertPolicy(
                policyService.readDocumentRechten(drcClientService.readEnkelvoudigInformatieobject(uuid)).lezen
            )
        }
        .let(drcClientService::listAuditTrail)
        .let(historieRegelConverter::convert)

    @POST
    @Path("/documentcreatie")
    fun createDocument(restDocumentCreatieGegevens: RESTDocumentCreatieGegevens): RESTDocumentCreatieResponse {
        val zaak = zrcClientService.readZaak(restDocumentCreatieGegevens.zaakUUID)
        assertPolicy(policyService.readZaakRechten(zaak).creeerenDocument)

        // documents created by SmartDocuments are always of the type 'bijlage'
        // the zaaktype of the current zaak needs to be configured to be able to use this informatieObjectType
        val informatieObjectType = ztcClientService.readInformatieobjecttypen(zaak.zaaktype)
            .stream()
            .filter { ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_BIJLAGE == it.omschrijving }
            .findAny()
            .orElseThrow {
                RuntimeException(
                    "No informatieobjecttype with omschrijving " +
                        "'${ConfiguratieService.INFORMATIEOBJECTTYPE_OMSCHRIJVING_BIJLAGE}' found for " +
                        "zaaktype '${zaak.zaaktype}'"
                )
            }

        return DocumentCreatieGegevens(
            zaak,
            restDocumentCreatieGegevens.taskId,
            informatieObjectType
        )
            .let(documentCreatieService::creeerDocumentAttendedSD)
            .let { RESTDocumentCreatieResponse(it.redirectUrl, it.message) }
    }

    @GET
    @Path("informatieobject/{informatieObjectUuid}/zaakidentificaties")
    fun listZaakIdentificatiesForInformatieobject(
        @PathParam("informatieObjectUuid") informatieobjectUuid: UUID
    ): List<String> =
        drcClientService.readEnkelvoudigInformatieobject(informatieobjectUuid)
            .apply { assertPolicy(policyService.readDocumentRechten(this).lezen) }
            .let(zrcClientService::listZaakinformatieobjecten)
            .map { zaakInformatieobject -> zrcClientService.readZaak(zaakInformatieobject.zaak).identificatie }
            .toList()

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
        @PathParam("uuid") enkelvoudigInformatieobjectUUID: UUID?,
        @QueryParam("zaak") zaakUUID: UUID?
    ): Response {
        val document = drcClientService.readEnkelvoudigInformatieobject(enkelvoudigInformatieobjectUUID)
        assertPolicy(
            policyService.readDocumentRechten(document, zrcClientService.readZaak(zaakUUID)).wijzigen
        )
        drcClientService.downloadEnkelvoudigInformatieobject(
            enkelvoudigInformatieobjectUUID
        ).use { documentInputStream ->
            officeConverterClientService.convertToPDF(
                documentInputStream,
                document
                    .bestandsnaam
            ).use { pdfInputStream ->
                val pdf = EnkelvoudigInformatieObjectWithLockRequest()
                val inhoud = pdfInputStream.readAllBytes()
                pdf.inhoud = convertByteArrayToBase64String(inhoud)
                pdf.formaat = MEDIA_TYPE_PDF
                pdf.bestandsnaam = StringUtils.substringBeforeLast(document.bestandsnaam, ".") + ".pdf"
                pdf.bestandsomvang = inhoud.size
                enkelvoudigInformatieObjectUpdateService.updateEnkelvoudigInformatieObjectWithLockData(
                    URIUtil.parseUUIDFromResourceURI(document.url),
                    pdf,
                    TOELICHTING_PDF
                )
            }
        }
        return Response.ok().build()
    }

    private fun isVerzendenToegestaan(informatieobject: EnkelvoudigInformatieObject): Boolean =
        informatieobject.vertrouwelijkheidaanduiding.let {
            informatieobject.status == StatusEnum.DEFINITIEF &&
                it != VertrouwelijkheidaanduidingEnum.CONFIDENTIEEL &&
                it != VertrouwelijkheidaanduidingEnum.GEHEIM &&
                it != VertrouwelijkheidaanduidingEnum.ZEER_GEHEIM &&
                informatieobject.ontvangstdatum == null &&
                MEDIA_TYPE_PDF == informatieobject.formaat
        }

    private fun listEnkelvoudigInformatieobjectenVoorZaak(zaak: Zaak): MutableList<RESTEnkelvoudigInformatieobject> =
        zaak.let(zrcClientService::listZaakinformatieobjecten)
            .map(informatieobjectConverter::convertToREST)
            .toMutableList()

    private fun listGekoppeldeZaakEnkelvoudigInformatieobjectenVoorZaak(
        zaakURI: URI,
        relatieType: RelatieType
    ): List<RESTGekoppeldeZaakEnkelvoudigInformatieObject> =
        zaakURI.let(zrcClientService::readZaak)
            .let { zaak ->
                zrcClientService.listZaakinformatieobjecten(zaak)
                    .map { informatieobjectConverter.convertToREST(it, relatieType, zaak) }
                    .toList()
            }

    private fun listGekoppeldeZaakInformatieObjectenVoorZaak(
        zaak: Zaak
    ): List<RESTGekoppeldeZaakEnkelvoudigInformatieObject> =
        mutableListOf<RESTGekoppeldeZaakEnkelvoudigInformatieObject>().apply {
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
                        gerelateerdeZaakConverter.convertToRelatieType(it.aardRelatie)
                    )
                )
            }
        }
}
