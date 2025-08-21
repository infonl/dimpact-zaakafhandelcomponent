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
import jakarta.servlet.http.HttpSession
import net.atos.zac.admin.ZaakafhandelParameterService
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.exception.PabcRuntimeException
import nl.info.client.pabc.model.generated.GetApplicationRolesResponse
import nl.info.zac.identity.model.ZACRole
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.jvm.java

const val REFRESH_TOKEN_ATTRIBUTE = "refresh_token"

@ApplicationScoped
@WebFilter(filterName = "UserPrincipalFilter")
@AllOpen
@NoArgConstructor
class UserPrincipalFilter
@Inject
constructor(
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val pabcClientService: PabcClientService,
    @ConfigProperty(name = "FEATURE_FLAG_PABC_INTEGRATION", defaultValue = "false")
    private val pabcIntegrationEnabled: Boolean
) : Filter {
    companion object {
        private val LOG = Logger.getLogger(UserPrincipalFilter::class.java.name)
        private const val GROUP_MEMBERSHIP_CLAIM_NAME = "group_membership"
        val SUPPORTED_PABC_FUNCTIONAL_ROLES: Set<String> = setOf(
            "behandelaar",
            "coordinator",
            "beheerder",
            "recordmanager",
            "raadpleger"
        )
    }

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        (servletRequest as? HttpServletRequest)?.userPrincipal?.let { userPrincipal ->
            val httpSession = servletRequest.getSession(true)

            getLoggedInUser(httpSession)?.let { loggedInUser ->
                if (loggedInUser.id != userPrincipal.name) {
                    LOG.info(
                        "Invalidating HTTP session of user '${loggedInUser.id}' " +
                            "on context path '${servletRequest.servletContext.contextPath}'"
                    )
                    httpSession.invalidate()
                    setLoggedInUserOnHttpSession(
                        userPrincipal as OidcPrincipal<*>,
                        servletRequest.getSession(true)
                    )
                }
            }
                ?: run {
                    // no logged-in user in session
                    setLoggedInUserOnHttpSession(userPrincipal as OidcPrincipal<*>, httpSession)
                }
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }

    fun setLoggedInUserOnHttpSession(
        oidcPrincipal: OidcPrincipal<*>,
        httpSession: HttpSession
    ) =
        createLoggedInUser(oidcPrincipal.oidcSecurityContext).let { loggedInUser ->
            setLoggedInUser(httpSession, loggedInUser)
            if (!pabcIntegrationEnabled) {
                LOG.info(
                    "User logged in: '${loggedInUser.id}' with roles: ${loggedInUser.roles}, " +
                        "groups: ${loggedInUser.groupIds} and zaaktypen: ${
                            if (loggedInUser.isAuthorisedForAllZaaktypen()) {
                                "ELK-ZAAKTYPE"
                            } else {
                                loggedInUser.geautoriseerdeZaaktypen.toString()
                            }
                        }"
                )
            } else {
                LOG.info(
                    "User logged in: '${loggedInUser.id}' with groups: ${loggedInUser.groupIds}, " +
                        "and application roles per zaaktype: ${loggedInUser.applicationRolesPerZaaktype}"
                )
            }
            this.addRefreshTokenToHttpSession(oidcPrincipal, httpSession)
        }

    private fun addRefreshTokenToHttpSession(oidcPrincipal: OidcPrincipal<*>, httpSession: HttpSession) {
        if (oidcPrincipal.oidcSecurityContext is RefreshableOidcSecurityContext) {
            val refreshToken = (oidcPrincipal.oidcSecurityContext as RefreshableOidcSecurityContext).refreshToken
            httpSession.setAttribute(REFRESH_TOKEN_ATTRIBUTE, refreshToken)
            LOG.info("Added $REFRESH_TOKEN_ATTRIBUTE to the user session")
        }
    }

    private fun createLoggedInUser(oidcSecurityContext: OidcSecurityContext): LoggedInUser =
        oidcSecurityContext.token.let { accessToken ->
            val functionalRoles = accessToken.rolesClaim.toSet()
            val applicationRolesPerZaaktype: Map<String, Set<String>> =
                if (pabcIntegrationEnabled) {
                    buildApplicationRoleMappingsFromPabc(functionalRoles)
                } else {
                    emptyMap()
                }

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
                geautoriseerdeZaaktypen = getAuthorisedZaaktypen(functionalRoles),
                applicationRolesPerZaaktype = applicationRolesPerZaaktype
            )
        }

    /**
     * Build a map of zaaktype -> set(application role names) from PABC (when enabled).
     * - Only include results where entityType.type == "zaaktype"
     * - Key uses entityType.name
     */
    @Suppress("TooGenericExceptionCaught")
    private fun buildApplicationRoleMappingsFromPabc(functionalRoles: Set<String>): Map<String, Set<String>> {
        // Filter out roles we shouldn't send to PABC
        // Currently PABC has only the following roles defined
        val filteredFunctionalRoles = functionalRoles.filter {
            it in SUPPORTED_PABC_FUNCTIONAL_ROLES
        }

        if (filteredFunctionalRoles.isEmpty()) {
            LOG.warning("No functional roles to send to PABC after filtering, returning empty mapping")
            return emptyMap()
        }

        LOG.info("Roles to be sent to PABC: $filteredFunctionalRoles")

        return try {
            val response: GetApplicationRolesResponse =
                pabcClientService.getApplicationRoles(filteredFunctionalRoles)

            response.results
                .filter { it.entityType?.type.equals("zaaktype", ignoreCase = true) }
                .mapNotNull { res ->
                    val key = res.entityType?.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val roleNames = res.applicationRoles.mapNotNull { it.name }.toSet()
                    key to roleNames
                }
                .toMap()
        } catch (ex: Exception) {
            LOG.log(Level.SEVERE, "PABC application role lookup failed", ex)
            throw PabcRuntimeException("Failed to get application roles from PABC", ex)
        }
    }

    /**
     * Returns the active zaaktypen for which the user is authorised, or `null` if the user is
     * authorised for all zaaktypen.
     */
    private fun getAuthorisedZaaktypen(roles: Set<String>): Set<String>? =
        if (roles.contains(ZACRole.DOMEIN_ELK_ZAAKTYPE.value)) {
            null
        } else {
            zaakafhandelParameterService
                .listZaakafhandelParameters()
                // group by zaaktype omschrijving since this is the unique identifier for a
                // zaaktype
                // (not the zaaktype uuid since that changes for every version of a
                // zaaktype)
                .groupBy { it.zaaktypeOmschrijving }
                // get the zaakafhandelparameter with the latest creation date (= the active
                // one)
                .map { it.value.maxBy { value -> value.creatiedatum } }
                // filter out the zaakafhandelparameters that have a domain that is equal to
                // one of the user's (domain) roles
                .filter { it.domein != null && roles.contains(it.domein) }
                .map { it.zaaktypeOmschrijving }
                .toSet()
        }
}
