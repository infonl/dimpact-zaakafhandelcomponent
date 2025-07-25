/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
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
import nl.info.client.kvk.model.KvkSearchParameters
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
interface KvkSearchClient {

    /**
     * Search for a company ('bedrijf') in the KVK and return basic company data ('basisinformatie').
     * The result will contain a maximum of 1000 results
     */
    @GET
    fun getResults(@BeanParam kvkSearchParameters: KvkSearchParameters): Resultaat
}
