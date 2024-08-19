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
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import java.util.Set.copyOf
import java.util.logging.Logger
import java.util.stream.Collectors

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
        if (servletRequest is HttpServletRequest) {
            servletRequest.userPrincipal?.let { userPrincipal ->
                var httpSession = servletRequest.getSession(true)
                var loggedInUser = getLoggedInUser(httpSession)
                if (loggedInUser != null && loggedInUser.id != userPrincipal.name) {
                    LOG.info(
                        String.format(
                            "Invalidating HTTP session of user '%s' on context path '%s'",
                            loggedInUser.id,
                            servletRequest.servletContext.contextPath
                        )
                    )
                    httpSession.invalidate()
                    httpSession = servletRequest.getSession(true)
                    loggedInUser = null
                }
                if (loggedInUser == null) {
                    val newUser = createLoggedInUser((userPrincipal as OidcPrincipal<*>).oidcSecurityContext)
                    setLoggedInUser(httpSession, newUser)
                    LOG.info(
                        String.format(
                            "User logged in: '%s' with roles: %s, groups: %s en zaaktypen: %s",
                            newUser.id,
                            newUser.roles,
                            newUser.groupIds,
                            if (newUser.isGeautoriseerdVoorAlleZaaktypen)
                                "ELK-ZAAKTYPE"
                            else
                                newUser.geautoriseerdeZaaktypen
                        )
                    )
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }

    private fun createLoggedInUser(oidcSecurityContext: OidcSecurityContext): LoggedInUser {
        val accessToken = oidcSecurityContext.token
        val roles = copyOf(accessToken.rolesClaim)
        return LoggedInUser(
            accessToken.preferredUsername,
            accessToken.givenName,
            accessToken.familyName,
            accessToken.name,
            accessToken.email,
            roles,
            copyOf(accessToken.getStringListClaimValue(GROUP_MEMBERSHIP_CLAIM_NAME)),
            getAuthorisedZaaktypen(roles)
        )
    }

    private fun getAuthorisedZaaktypen(roles: Set<String>): Set<String>? {
        return if (roles.contains(ROL_DOMEIN_ELK_ZAAKTYPE)) {
            null
        } else {
            zaakafhandelParameterService.listZaakafhandelParameters().stream()
                .filter { zaakafhandelParameters: ZaakafhandelParameters ->
                    zaakafhandelParameters.domein != null &&
                        roles.contains(zaakafhandelParameters.domein)
                }
                .map { it.zaaktypeOmschrijving }
                .collect(Collectors.toUnmodifiableSet())
        }
    }
}
