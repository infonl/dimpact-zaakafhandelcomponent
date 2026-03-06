/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.model.generated.GetApplicationRolesRequest
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.client.pabc.model.generated.GroupRepresentation
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
@NoArgConstructor
@AllOpen
class PabcClientService @Inject constructor(
    @RestClient private val pabcClient: PabcClient
) {
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
}
