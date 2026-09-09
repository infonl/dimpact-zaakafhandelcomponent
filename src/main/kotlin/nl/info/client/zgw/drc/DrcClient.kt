/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.drc

import jakarta.json.JsonObject
import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM
import jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE
import jakarta.ws.rs.core.Response
import nl.info.client.zgw.shared.exception.ZgwErrorExceptionMapper
import nl.info.client.zgw.shared.exception.ZgwValidationErrorResponseExceptionMapper
import nl.info.client.zgw.shared.model.Results
import nl.info.client.zgw.shared.model.audit.AuditTrailRegel
import nl.info.client.zgw.util.JsonbConfiguration
import nl.info.client.zgw.drc.exception.DrcRuntimeResponseExceptionMapper
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.ObjectInformatieobjectListParameters
import nl.info.client.zgw.drc.model.generated.BestandsDeel
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockSub
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.drc.model.generated.LockEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.ObjectInformatieObject
import nl.info.client.zgw.util.ZgwClientHeadersFactory
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.io.InputStream
import java.util.UUID

@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZgwClientHeadersFactory::class)
@RegisterProvider(ZgwErrorExceptionMapper::class)
@RegisterProvider(ZgwValidationErrorResponseExceptionMapper::class)
@RegisterProvider(DrcRuntimeResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Path("documenten/api/v1")
@Produces(APPLICATION_JSON)
@Suppress("TooManyFunctions")
interface DrcClient {

    /**
     * The parts are announced by the documents registry in
     * [EnkelvoudigInformatieObject.getBestandsdelen] when a document is created with a
     * `bestandsomvang` but without `inhoud`.
     */
    @PUT
    @Path("bestandsdelen/{uuid}")
    fun bestandsdeelUpdate(
        @PathParam("uuid") uuid: UUID,
        @HeaderParam(CONTENT_TYPE) contentType: String,
        bestandsDeel: InputStream
    ): BestandsDeel

    @POST
    @Path("enkelvoudiginformatieobjecten")
    fun enkelvoudigInformatieobjectCreate(
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest
    ): EnkelvoudigInformatieObject

    /**
     * Creates a document whose content is uploaded in parts afterwards, which is what happens when
     * the request carries a `bestandsomvang` but no `inhoud`.
     *
     * The response is read as an [EnkelvoudigInformatieObjectCreateLockSub] rather than an
     * [EnkelvoudigInformatieObject] because only that carries the `lock` that the parts have to be
     * uploaded under.
     */
    @POST
    @Path("enkelvoudiginformatieobjecten")
    fun enkelvoudigInformatieobjectCreateForPartsUpload(
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest
    ): EnkelvoudigInformatieObjectCreateLockSub

    @GET
    @Path("enkelvoudiginformatieobjecten")
    fun enkelvoudigInformatieobjectList(
        @BeanParam parameters: EnkelvoudigInformatieobjectListParameters
    ): Results<EnkelvoudigInformatieObject>

    @GET
    @Path("enkelvoudiginformatieobjecten/{uuid}")
    fun enkelvoudigInformatieobjectRead(@PathParam("uuid") uuid: UUID): EnkelvoudigInformatieObject

    @GET
    @Path("enkelvoudiginformatieobjecten/{uuid}")
    fun enkelvoudigInformatieobjectReadVersie(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("versie") versie: Int
    ): EnkelvoudigInformatieObject

    @GET
    @Produces(APPLICATION_OCTET_STREAM)
    @Path("enkelvoudiginformatieobjecten/{uuid}/download")
    fun enkelvoudigInformatieobjectDownload(@PathParam("uuid") uuid: UUID): Response

    @GET
    @Produces(APPLICATION_OCTET_STREAM)
    @Path("enkelvoudiginformatieobjecten/{uuid}/download")
    fun enkelvoudigInformatieobjectDownloadVersie(
        @PathParam("uuid") uuid: UUID,
        @QueryParam("versie") versie: Int
    ): Response

    @PATCH
    @Path("enkelvoudiginformatieobjecten/{uuid}")
    fun enkelvoudigInformatieobjectPartialUpdate(
        @PathParam("uuid") uuid: UUID,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest
    ): EnkelvoudigInformatieObject

    /**
     * Replaces the content of a document with content that is uploaded in parts, so that the
     * request carries a `bestandsomvang` but no `inhoud`.
     *
     * The body is assembled by the caller instead of being taken as a typed request because it has
     * to carry `inhoud` as an explicit `null`. JSON-B leaves a null property out of the request
     * altogether, and to the documents registry an absent `inhoud` on a partial update means "keep
     * the content that is there", which then no longer matches the new `bestandsomvang`.
     */
    @PATCH
    @Path("enkelvoudiginformatieobjecten/{uuid}")
    fun enkelvoudigInformatieobjectPartialUpdateForPartsUpload(
        @PathParam("uuid") uuid: UUID,
        body: JsonObject
    ): EnkelvoudigInformatieObject

    @DELETE
    @Path("enkelvoudiginformatieobjecten/{uuid}")
    fun enkelvoudigInformatieobjectDelete(@PathParam("uuid") uuid: UUID): Response

    @POST
    @Path("enkelvoudiginformatieobjecten/{uuid}/lock")
    fun enkelvoudigInformatieobjectLock(
        @PathParam("uuid") uuid: UUID,
        enkelvoudigInformatieObjectLock: LockEnkelvoudigInformatieObject
    ): LockEnkelvoudigInformatieObject

    @POST
    @Path("enkelvoudiginformatieobjecten/{uuid}/unlock")
    fun enkelvoudigInformatieobjectUnlock(
        @PathParam("uuid") uuid: UUID,
        lock: LockEnkelvoudigInformatieObject
    ): Response

    @POST
    @Path("gebruiksrechten")
    fun gebruiksrechtenCreate(gebruiksrechten: Gebruiksrechten): Gebruiksrechten

    @GET
    @Path("objectinformatieobjecten")
    fun objectInformatieobjectList(
        @BeanParam parameters: ObjectInformatieobjectListParameters
    ): Results<ObjectInformatieObject>

    @GET
    @Path("enkelvoudiginformatieobjecten/{uuid}/audittrail")
    fun listAuditTrail(@PathParam("uuid") enkelvoudigInformatieobjectUUID: UUID): List<AuditTrailRegel>
}
