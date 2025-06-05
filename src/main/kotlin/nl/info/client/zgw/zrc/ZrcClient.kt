/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc

import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.zgw.shared.exception.ZgwErrorExceptionMapper
import net.atos.client.zgw.shared.exception.ZgwValidationErrorResponseExceptionMapper
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.JsonbConfiguration
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolListParameters
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import nl.info.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import nl.info.client.zgw.zrc.exception.ZrcResponseExceptionMapper
import nl.info.client.zgw.zrc.model.ZaakUuid
import nl.info.client.zgw.zrc.model.generated.Resultaat
import nl.info.client.zgw.zrc.model.generated.Status
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakEigenschap
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParams
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.UUID

@RegisterRestClient(configKey = "ZGW-API-Client")
@RegisterClientHeaders(ZGWClientHeadersFactory::class)
@RegisterProvider(ZgwErrorExceptionMapper::class)
@RegisterProvider(ZgwValidationErrorResponseExceptionMapper::class)
@RegisterProvider(ZrcResponseExceptionMapper::class)
@RegisterProvider(JsonbConfiguration::class)
@Path("zaken/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Suppress("TooManyFunctions")
interface ZrcClient {
    companion object {
        const val ACCEPT_CRS = "Accept-Crs"
        const val ACCEPT_CRS_VALUE = "EPSG:4326"
        const val CONTENT_CRS = "Content-Crs"
        const val CONTENT_CRS_VALUE = ACCEPT_CRS_VALUE
    }

    @GET
    @Path("zaken")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun zaakList(@BeanParam parameters: ZaakListParameters): Results<Zaak>

    @GET
    @Path("zaken")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun zaakListUuids(@BeanParam parameters: ZaakListParameters): Results<ZaakUuid>

    @POST
    @Path("zaken")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun zaakCreate(zaak: Zaak): Zaak

    @PATCH
    @Path("zaken/{uuid}")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun zaakPartialUpdate(@PathParam("uuid") uuid: UUID, zaak: Zaak): Zaak

    @GET
    @Path("zaken/{uuid}")
    @ClientHeaderParams(
        ClientHeaderParam(name = ACCEPT_CRS, value = arrayOf(ACCEPT_CRS_VALUE)),
        ClientHeaderParam(name = CONTENT_CRS, value = arrayOf(CONTENT_CRS_VALUE))
    )
    fun zaakRead(@PathParam("uuid") uuid: UUID): Zaak

    @GET
    @Path("rollen")
    fun rolList(@BeanParam parameters: RolListParameters): Results<Rol<*>>

    @POST
    @Path("rollen")
    fun rolCreate(rol: Rol<*>): Rol<*>

    @DELETE
    @Path("rollen/{uuid}")
    fun rolDelete(@PathParam("uuid") uuid: UUID)

    @GET
    @Path("rollen/{uuid}")
    fun rolRead(@PathParam("uuid") uuid: UUID): Rol<*>

    @GET
    @Path("zaakinformatieobjecten")
    fun zaakinformatieobjectList(@BeanParam parameters: ZaakInformatieobjectListParameters): List<ZaakInformatieobject>

    @POST
    @Path("zaakinformatieobjecten")
    fun zaakinformatieobjectCreate(zaakInformatieObject: ZaakInformatieobject): ZaakInformatieobject

    @DELETE
    @Path("zaakinformatieobjecten/{uuid}")
    fun zaakinformatieobjectDelete(@PathParam("uuid") uuid: UUID)

    @POST
    @Path("statussen")
    fun statusCreate(status: Status): Status

    @GET
    @Path("statussen/{status_uuid}")
    fun statusRead(@PathParam("status_uuid") statusUUID: UUID): Status

    @POST
    @Path("resultaten")
    fun resultaatCreate(resultaat: Resultaat): Resultaat

    @GET
    @Path("resultaten/{uuid}")
    fun resultaatRead(@PathParam("uuid") resultaatUUID: UUID): Resultaat

    @PUT
    @Path("resultaten/{uuid}")
    fun resultaatUpdate(@PathParam("uuid") resultaatUUID: UUID, resultaat: Resultaat): Resultaat

    @DELETE
    @Path("resultaten/{uuid}")
    fun resultaatDelete(@PathParam("uuid") uuid: UUID)

    @GET
    @Path("zaken/{zaak_uuid}/zaakeigenschappen")
    fun zaakeigenschapList(@PathParam("zaak_uuid") zaakUUID: UUID): List<ZaakEigenschap>

    @POST
    @Path("zaken/{zaak_uuid}/zaakeigenschappen")
    fun zaakeigenschapCreate(@PathParam("zaak_uuid") zaakUUID: UUID, zaakeigenschap: ZaakEigenschap): ZaakEigenschap

    @GET
    @Path("zaakobjecten")
    fun zaakobjectList(@BeanParam zaakobjectListParameters: ZaakobjectListParameters): Results<Zaakobject>

    @POST
    @Path("zaakobjecten")
    fun zaakobjectCreate(zaakobject: Zaakobject): Zaakobject

    @DELETE
    @Path("zaakobjecten/{uuid}")
    fun zaakobjectDelete(@PathParam("uuid") uuid: UUID)

    @GET
    @Path("zaakobjecten/{uuid}")
    fun zaakobjectRead(@PathParam("uuid") uuid: UUID): Zaakobject

    @GET
    @Path("zaken/{zaak_uuid}/audittrail")
    fun listAuditTrail(@PathParam("zaak_uuid") zaakUUID: UUID): List<ZRCAuditTrailRegel>

    @GET
    @Path("zaakinformatieobjecten/{uuid}")
    fun zaakinformatieobjectRead(@PathParam("uuid") zaakinformatieobjectUUID: UUID): ZaakInformatieobject
}
