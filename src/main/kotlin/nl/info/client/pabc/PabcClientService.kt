/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.zac.identity.model.FunctionalRole
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class PabcClientService @Inject constructor(
    @RestClient private val pabcClient: PabcClient,
    @ConfigProperty(name = "FEATURE_FLAG_PABC_INTEGRATION", defaultValue = "false")
    private val pabcIntegrationEnabled: Boolean
) {
    companion object {
        private val LOG = Logger.getLogger(PabcClientService::class.java.name)
    }

    fun getApplicationRoles(
        functionalRoles: Collection<String>,
        rolesToBeFiltered: Collection<FunctionalRole>? = null
    ): GetApplicationRolesResponse? {
        if (!pabcIntegrationEnabled) {
            LOG.info("PABC integration is disabled — skipping application role lookup.")
            return null
        }

        val filteredRoles = functionalRoles.filterNot { role ->
            role in (rolesToBeFiltered?.map { it.value }?.toSet() ?: emptySet())
        }
        val applicationRolesRequest = GetApplicationRolesRequest().apply {
            functionalRoleNames = filteredRoles.toList()
        }
        return pabcClient.getApplicationRolesPerEntityType(applicationRolesRequest)
    }
}
