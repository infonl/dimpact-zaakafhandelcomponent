/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk

import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import net.atos.zac.util.MediaTypes
import nl.info.client.kvk.exception.KvkRuntimeExceptionMapper
import nl.info.client.kvk.util.KvkClientHeadersFactory
import nl.info.client.kvk.vestigingsprofiel.model.generated.Vestiging
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KvkClientHeadersFactory::class)
@RegisterProvider(KvkRuntimeExceptionMapper::class)
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON)
@Path("api/v1/vestigingsprofielen")
interface KvkVestigingsprofielClient {
    /**
     * Voor een specifieke vestiging informatie opvragen.
     */
    @GET
    @Path("{vestigingsnummer}")
    fun getVestigingByVestigingsnummer(
        @PathParam("vestigingsnummer") vestigingsnummer: String,
        @QueryParam("geoData") @DefaultValue("false") geoData: Boolean
    ): Vestiging
}
