/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.authentication

import jakarta.inject.Inject
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import net.atos.zac.admin.ZaakafhandelParameterService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import java.util.logging.Logger

@WebFilter(filterName = "UserPrincipalFilter")
@AllOpen
@NoArgConstructor
class UserPrincipalFilter @Inject constructor(
    private val zaakafhandelParameterService: ZaakafhandelParameterService
) : Filter {
    companion object {
        private val LOG: Logger = Logger.getLogger(UserPrincipalFilter::class.java.name)
        private const val ROL_DOMEIN_ELK_ZAAKTYPE = "domein_elk_zaaktype"
        private const val GROUP_MEMBERSHIP_CLAIM_NAME = "group_membership"
    }

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        (servletRequest as? HttpServletRequest)?.userPrincipal?.let { userPrincipal ->
            var httpSession = servletRequest.getSession(true)
            getLoggedInUser(httpSession)?.let { loggedInUser ->
                if (loggedInUser.id != userPrincipal.name) {
                    LOG.info(
                        "Invalidating HTTP session of user '${loggedInUser.id}' " +
                            "on context path '${servletRequest.servletContext.contextPath}'"
                    )
                    httpSession.invalidate()
                    httpSession = servletRequest.getSession(true)
                    addLoggedInUserToHttpSession(userPrincipal as OidcPrincipal<*>, httpSession)
                }
            } ?: run {
                // no logged-in user in session
                addLoggedInUserToHttpSession(userPrincipal as OidcPrincipal<*>, httpSession)
            }
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }

    private fun addLoggedInUserToHttpSession(oidcPrincipal: OidcPrincipal<*>, httpSession: HttpSession) =
        createLoggedInUser(oidcPrincipal.oidcSecurityContext).let { loggedInUser ->
            setLoggedInUser(httpSession, loggedInUser)
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
        }

    private fun createLoggedInUser(oidcSecurityContext: OidcSecurityContext): LoggedInUser =
        oidcSecurityContext.token.let { accessToken ->
            accessToken.rolesClaim.toSet().let { roles ->
                LoggedInUser(
                    accessToken.preferredUsername,
                    accessToken.givenName,
                    accessToken.familyName,
                    accessToken.name,
                    accessToken.email,
                    roles,
                    accessToken.getStringListClaimValue(GROUP_MEMBERSHIP_CLAIM_NAME).toSet(),
                    getAuthorisedZaaktypen(roles)
                )
            }
        }

    /**
     * Returns the zaaktypen for which the user is authorised, or `null` if the user is authorised for all zaaktypen.
     */
    private fun getAuthorisedZaaktypen(roles: Set<String>): Set<String>? =
        if (roles.contains(ROL_DOMEIN_ELK_ZAAKTYPE)) {
            null
        } else {
            zaakafhandelParameterService.listZaakafhandelParameters()
                .filter { it.domein != null && roles.contains(it.domein) }
                .map { it.zaaktypeOmschrijving }
                .toSet()
        }
}
