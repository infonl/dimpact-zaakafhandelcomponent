/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk

import jakarta.ws.rs.BeanParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import net.atos.zac.util.MediaTypes
import nl.info.client.kvk.exception.KvkClientNoResultResponseExceptionMapper
import nl.info.client.kvk.exception.KvkRuntimeExceptionMapper
import nl.info.client.kvk.model.KvkZoekenParameters
import nl.info.client.kvk.util.KvkClientHeadersFactory
import nl.info.client.kvk.zoeken.model.generated.Resultaat
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import java.time.temporal.ChronoUnit

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KvkClientHeadersFactory::class)
@RegisterProvider(KvkRuntimeExceptionMapper::class)
@RegisterProvider(KvkClientNoResultResponseExceptionMapper::class)
@Produces(MediaTypes.MEDIA_TYPE_HAL_JSON)
@Path("api/v2/zoeken")
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
interface ZoekenClient {
    /**
     * Voor een bedrijf zoeken naar basisinformatie.
     *
     *
     * Er wordt max. 1000 resultaten getoond.
     */
    @GET
    fun getResults(@BeanParam zoekenParameters: KvkZoekenParameters): Resultaat
}
