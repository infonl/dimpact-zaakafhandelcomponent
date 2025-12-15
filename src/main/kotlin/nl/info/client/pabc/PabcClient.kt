/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.util.PabcClientHeadersFactory
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

/**
 * Constant for PABC entity type 'zaaktype'.
 * In a future version of the PABC this will be replaced by an enum in the PABC API.
 */
const val ENTITY_TYPE_ZAAKTYPE = "ZAAKTYPE"

@RegisterRestClient(configKey = "PABC-API-Client")
@RegisterClientHeaders(PabcClientHeadersFactory::class)
@Path("/api/v1")
interface PabcClient {
    @POST
    @Path("/application-roles-per-entity-type")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getApplicationRolesPerEntityType(request: GetApplicationRolesRequest): GetApplicationRolesResponse
}
