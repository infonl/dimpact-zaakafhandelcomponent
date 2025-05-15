/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.inject.Inject
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import net.atos.zac.admin.ZaakafhandelParameterService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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
        private val LOG = Logger.getLogger(UserPrincipalFilter::class.java.name)
        private const val GROUP_MEMBERSHIP_CLAIM_NAME = "group_membership"
        const val ROL_DOMEIN_ELK_ZAAKTYPE = "domein_elk_zaaktype"
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
                    addLoggedInUserToHttpSession(userPrincipal as OidcPrincipal<*>, servletRequest.getSession(true))
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
                    id = accessToken.preferredUsername,
                    firstName = accessToken.givenName,
                    lastName = accessToken.familyName,
                    displayName = accessToken.name,
                    email = accessToken.email,
                    roles = roles,
                    groupIds = accessToken.getStringListClaimValue(GROUP_MEMBERSHIP_CLAIM_NAME).toSet(),
                    geautoriseerdeZaaktypen = getAuthorisedZaaktypen(roles)
                )
            }
        }

    /**
     * Returns the active zaaktypen for which the user is authorised, or `null` if the user is authorised for all zaaktypen.
     */
    private fun getAuthorisedZaaktypen(roles: Set<String>): Set<String>? =
        if (roles.contains(ROL_DOMEIN_ELK_ZAAKTYPE)) {
            null
        } else {
            zaakafhandelParameterService.listZaakafhandelParameters()
                // group by zaakttype omschrijving since this is the unique identifier for a zaaktype
                // (not the zaaktype uuid since that changes for every version of a zaaktype)
                .groupBy { it.zaaktypeOmschrijving }
                // get the zaakafhandelparameter with the latest creation date (= the active one)
                .map { it.value.maxBy { value -> value.creatiedatum } }
                // filter out the zaakafhandelparameters that have a domain that is equal to one of the user's (domain) roles
                .filter { it.domein != null && roles.contains(it.domein) }
                .map { it.zaaktypeOmschrijving }
                .toSet()
        }
}
