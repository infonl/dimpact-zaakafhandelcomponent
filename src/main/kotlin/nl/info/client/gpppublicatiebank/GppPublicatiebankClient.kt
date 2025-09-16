/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.gpppublicatiebank

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.client.gpppublicatiebank.PublicationRead
import net.atos.client.gpppublicatiebank.PublicationWrite
import nl.info.client.pabc.util.PabcClientHeadersFactory
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "GPP-PUBLICATIEBANK-API-Client")
@RegisterClientHeaders(PabcClientHeadersFactory::class)
@Path("/api/v2")
interface GppPublicatiebankClient {
    @POST
    @Path("/publicaties")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createPublicatie(request: PublicationWrite): PublicationRead
}

