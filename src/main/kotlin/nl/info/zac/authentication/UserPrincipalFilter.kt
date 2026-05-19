/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import nl.info.client.pabc.ENTITY_TYPE_ZAAKTYPE
import nl.info.client.pabc.PabcClientService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import java.util.logging.Logger

private data class ApplicationRoleMappings(
    val rolesPerZaaktype: Map<String, Set<String>>,
    val overallRoles: Set<String>,
)

@ApplicationScoped
@WebFilter(filterName = "UserPrincipalFilter")
@AllOpen
@NoArgConstructor
class UserPrincipalFilter
@Inject
constructor(
    private val pabcClientService: PabcClientService
) : Filter {
    companion object {
        private val LOG = Logger.getLogger(UserPrincipalFilter::class.java.name)
        private const val GROUP_MEMBERSHIP_CLAIM_NAME = "group_membership"
    }

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        servletRequest as HttpServletRequest
        servletResponse as HttpServletResponse

        servletRequest.userPrincipal?.let { userPrincipal ->
            val httpSession = servletRequest.getSession(true)

            getLoggedInUser(httpSession)?.let { loggedInUser ->
                if (loggedInUser.id != userPrincipal.name) {
                    LOG.info {
                        "Invalidating HTTP session of user '${loggedInUser.id}' " +
                            "on context path '${servletRequest.servletContext.contextPath}'. " +
                            "Creating new session for user with user principal name '${userPrincipal.name}'."
                    }
                    httpSession.invalidate()
                    setLoggedInUserOnHttpSession(
                        userPrincipal as OidcPrincipal<*>,
                        servletRequest.getSession(true)
                    )
                }
            } ?: run {
                // no logged-in user in session
                setLoggedInUserOnHttpSession(userPrincipal as OidcPrincipal<*>, httpSession)
            }
        }

        filterChain.doFilter(servletRequest, servletResponse)
    }

    fun setLoggedInUserOnHttpSession(
        oidcPrincipal: OidcPrincipal<*>,
        httpSession: HttpSession
    ) = createLoggedInUser(oidcPrincipal.oidcSecurityContext).let { loggedInUser ->
        setLoggedInUser(httpSession, loggedInUser)
        LOG.info {
            "User logged in: '${loggedInUser.id}' with groups: ${loggedInUser.groupIds}, " +
                "functional roles: '${loggedInUser.roles}' " +
                "and application roles per zaaktype: ${loggedInUser.applicationRolesPerZaaktype}, " +
                "overall roles: ${loggedInUser.overallRoles}"
        }
    }

    private fun createLoggedInUser(oidcSecurityContext: OidcSecurityContext): LoggedInUser =
        oidcSecurityContext.token.let { accessToken ->
            // functional roles are Keycloak realm roles
            val functionalRoles = accessToken.realmAccessClaim?.roles?.toSet() ?: emptySet()
            val applicationRoleMappings = if (functionalRoles.isNotEmpty()) {
                buildApplicationRoleMappingsFromPabc(functionalRoles)
            } else {
                ApplicationRoleMappings(rolesPerZaaktype = emptyMap(), overallRoles = emptySet())
            }
            val applicationRolesPerZaaktype: Map<String, Set<String>> = applicationRoleMappings.rolesPerZaaktype

            LoggedInUser(
                id = accessToken.preferredUsername,
                firstName = accessToken.givenName,
                lastName = accessToken.familyName,
                displayName = accessToken.name,
                email = accessToken.email,
                roles = functionalRoles,
                groupIds = accessToken
                    .getStringListClaimValue(GROUP_MEMBERSHIP_CLAIM_NAME)
                    .toSet(),
                applicationRolesPerZaaktype = applicationRolesPerZaaktype,
                overallRoles = applicationRoleMappings.overallRoles,
            )
        }

    /**
     * Builds [ApplicationRoleMappings] from the PABC response for the given functional roles.
     * - [ApplicationRoleMappings.rolesPerZaaktype]: results with a 'ZAAKTYPE' entity type, keyed by entityType.id.
     * - [ApplicationRoleMappings.overallRoles]: results without an entity type (apply to all entity types).
     */
    private fun buildApplicationRoleMappingsFromPabc(
        functionalRoles: Set<String>
    ): ApplicationRoleMappings {
        val applicationRolesResponse = pabcClientService.getApplicationRoles(functionalRoles.toList())
        val rolesPerZaaktype: Map<String, Set<String>> =
            applicationRolesResponse.results
                .filter {
                    it.entityType?.type.equals(ENTITY_TYPE_ZAAKTYPE, ignoreCase = true) &&
                        !it.entityType?.id.isNullOrBlank()
                }.associate { result ->
                    val roles = result.applicationRoles
                        .mapNotNull { it.name?.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                    result.entityType.id to roles
                }

        val overallRoles: Set<String> =
            applicationRolesResponse.results
                // an 'application roles response model' without an entity type means that these application roles
                // apply to all entity types (and hence all zaaktypen)
                .filter { it.entityType == null }
                .flatMap { it.applicationRoles.mapNotNull { applicationRoleModel -> applicationRoleModel.name?.trim() } }
                .filter { it.isNotEmpty() }
                .toSet()

        return ApplicationRoleMappings(
            rolesPerZaaktype = rolesPerZaaktype,
            overallRoles = overallRoles
        )
    }
}
