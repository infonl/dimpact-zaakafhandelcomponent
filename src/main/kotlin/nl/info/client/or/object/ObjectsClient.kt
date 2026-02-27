/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("PackageName")

package nl.info.client.or.`object`

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON
import net.atos.client.or.shared.exception.FoutExceptionMapper
import net.atos.client.or.shared.exception.ORRuntimeResponseExceptionMapper
import net.atos.client.or.shared.exception.ValidatieFoutExceptionMapper
import nl.info.client.or.objects.model.generated.ModelObject
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.util.UUID

@RegisterRestClient(configKey = "Objects-API-Client")
@RegisterClientHeaders(ObjectsClientHeadersFactory::class)
@RegisterProvider(FoutExceptionMapper::class)
@RegisterProvider(ValidatieFoutExceptionMapper::class)
@RegisterProvider(ORRuntimeResponseExceptionMapper::class)
@Produces(APPLICATION_JSON)
@Path("api/v2")
interface ObjectsClient {
    companion object {
        const val ACCEPT_CRS_VALUE = "EPSG:4326"
    }

    @GET
    @Path("objects/{object-uuid}")
    fun objectRead(@PathParam("object-uuid") objectUUID: UUID): ModelObject
}
