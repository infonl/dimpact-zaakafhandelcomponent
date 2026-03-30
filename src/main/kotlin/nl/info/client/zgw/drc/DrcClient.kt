/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.drc

import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM
import jakarta.ws.rs.core.Response
import net.atos.client.zgw.shared.exception.ZgwErrorExceptionMapper
import net.atos.client.zgw.shared.exception.ZgwValidationErrorResponseExceptionMapper
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.util.JsonbConfiguration
import nl.info.client.zgw.drc.exception.DrcRuntimeResponseExceptionMapper
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.drc.model.ObjectInformatieobjectListParameters
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectWithLockRequest
import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.drc.model.generated.LockEnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.ObjectInformatieObject
import nl.info.client.zgw.util.ZgwClientHeadersFactory
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
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

    @POST
    @Path("enkelvoudiginformatieobjecten")
    fun enkelvoudigInformatieobjectCreate(
        enkelvoudigInformatieObjectCreateLockRequest: EnkelvoudigInformatieObjectCreateLockRequest
    ): EnkelvoudigInformatieObject

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
        @QueryParam("versie") versie: Int?
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
        @QueryParam("versie") versie: Int?
    ): Response

    @PATCH
    @Path("enkelvoudiginformatieobjecten/{uuid}")
    fun enkelvoudigInformatieobjectPartialUpdate(
        @PathParam("uuid") uuid: UUID,
        enkelvoudigInformatieObjectWithLockRequest: EnkelvoudigInformatieObjectWithLockRequest
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
