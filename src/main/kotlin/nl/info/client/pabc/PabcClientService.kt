/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@NoArgConstructor
@AllOpen
class PabcClientService @Inject constructor(
    @RestClient private val pabcClient: PabcClient
) {

    fun getApplicationRoles(functionalRoles: List<String>): GetApplicationRolesResponse {
        val applicationRolesRequest = GetApplicationRolesRequest().apply {
            functionalRoleNames = functionalRoles
        }
        return pabcClient.getApplicationRolesPerEntityType(applicationRolesRequest)
    }
}
