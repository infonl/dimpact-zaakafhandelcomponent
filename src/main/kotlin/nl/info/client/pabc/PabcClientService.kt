/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.model.generated.GroupRepresentation
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.logging.Logger
import kotlin.jvm.java

@ApplicationScoped
@NoArgConstructor
@AllOpen
class PabcClientService @Inject constructor(
    @RestClient private val pabcClient: PabcClient,

    @ConfigProperty(name = "FEATURE_FLAG_PABC_INTEGRATION", defaultValue = "false")
    private val pabcIntegrationEnabled: Boolean
) {
    companion object {
        private const val FAKE_FUNCTIONAL_ROLE = "FAKE_FUNCTIONAL_ROLE"
        private val LOG = Logger.getLogger(PabcClientService::class.java.name)
    }

    fun getApplicationRoles(functionalRoles: List<String>): GetApplicationRolesResponse =
        pabcClient.getApplicationRolesPerEntityType(
            GetApplicationRolesRequest().apply { functionalRoleNames = functionalRoles }
        )

    /**
     * Returns the list of groups that are authorised for the given ZAC application role and zaaktype description.
     * The PABC determines this list based on the functional roles of the available groups in Keycloak, and
     * the PABC functional role authorisation mappings.
     */
    fun getGroupsByApplicationRoleAndZaaktype(
        applicationRole: String,
        zaaktypeDescription: String
    ): List<GroupRepresentation> = pabcClient.getGroupsByApplicationRoleAndEntityType(
        applicationName = APPLICATION_NAME_ZAC,
        applicationRoleName = applicationRole,
        entityTypeId = zaaktypeDescription,
        entityType = ENTITY_TYPE_ZAAKTYPE
    ).groups

    /**
     * Attempt to call the PABC API on application startup, using a (most likely non-existing) fake functional role,
     * to ensure that the PABC API is available and properly configured.
     * If the PABC API is not available or not properly configured,
     * this will result in an exception being thrown on application startup,
     * which will prevent ZAC from starting up successfully.
     */
    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        if (pabcIntegrationEnabled) {
            LOG.info { "PABC integration is enabled, attempting to call the PABC API to verify connectivity and configuration" }
            getApplicationRoles(listOf(FAKE_FUNCTIONAL_ROLE))
        } else {
            LOG.info { "PABC integration is disabled, not attempting to call the PABC API" }
        }
    }
}
