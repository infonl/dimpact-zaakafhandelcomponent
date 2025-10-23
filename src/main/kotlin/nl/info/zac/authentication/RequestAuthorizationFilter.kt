/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
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
import jakarta.ws.rs.HttpMethod.DELETE
import jakarta.ws.rs.HttpMethod.GET
import jakarta.ws.rs.HttpMethod.POST
import nl.info.zac.identity.model.ZacApplicationRole
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * A post-authorization gate for ZAC HTTP requests.
 * Allows an explicit set of public endpoints without a logged-in user.
 * For every other request, it expects authentication already completed
 *   (LoggedInUser is in the session) and enforces PABC/legacy rules.
 *   - PABC ON (`FEATURE_FLAG_PABC_INTEGRATION=true`)
 *      General access: user must have at least one application role on at least one zaaktype.
 *      For admin URIs (/admin/, /rest/admin/): User must have beheerder on some zaaktype
 *   - PABC OFF
 *      General access: user must have one of the legacy app roles
 *      (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) in token claims.
 *      For Admin URIs: user must have `beheerder` role in token claims.
 *
 * This filter must run after UserPrincipalFilter, so UserPrincipalFilter can
 * authenticate via Elytron OIDC and build the LoggedInUser in the session first.
 */
@ApplicationScoped
@WebFilter(filterName = "RequestAuthorizationFilter")
@AllOpen
@NoArgConstructor
class RequestAuthorizationFilter @Inject constructor(
    @ConfigProperty(name = "FEATURE_FLAG_PABC_INTEGRATION", defaultValue = "false")
    private val pabcIntegrationEnabled: Boolean
) : Filter {
    companion object {
        private val ADMIN_URI_PREFIXES = listOf(
            "/rest/admin/",
            "/admin",
        )
    }

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        val request = servletRequest as HttpServletRequest
        val response = servletResponse as HttpServletResponse
        if (!requestIsAllowed(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun requestIsAllowed(
        request: HttpServletRequest
    ): Boolean {
        val requestPath = request.requestURI.removePrefix(request.contextPath)
        val httpRequestMethod = request.method
        return when {
            requestPath == "/rest/notificaties" -> httpRequestMethod == POST
            requestPath.startsWith("/rest/internal/") -> httpRequestMethod == GET || httpRequestMethod == DELETE
            requestPath == "/websocket" -> httpRequestMethod == GET
            // allow unchecked access on '/webdav/*'
            requestPath.startsWith("/webdav/") -> true
            requestPath.startsWith("/rest/document-creation/smartdocuments/cmmn-callback/") -> httpRequestMethod == POST
            requestPath.startsWith("/rest/document-creation/smartdocuments/bpmn-callback/") -> httpRequestMethod == POST
            requestPath == "/static/smart-documents-result.html" -> httpRequestMethod == GET
            requestPath.startsWith("/assets/") -> httpRequestMethod == GET
            // for all other paths, authorization is required
            else -> isAuthorizationAllowed(request)
        }
    }

    @Suppress("ReturnCount")
    private fun isAuthorizationAllowed(request: HttpServletRequest): Boolean {
        val session = request.getSession(false) ?: return false
        val user = getLoggedInUser(session) ?: return false
        val path = request.requestURI.removePrefix(request.contextPath)
        val isAdmin = ADMIN_URI_PREFIXES.any(path::startsWith)
        return if (pabcIntegrationEnabled) {
            if (isAdmin) {
                // access allowed if the user has the 'beheerder' application role for at least one zaaktype
                hasAnyBeheerderApplicationRole(user)
            } else {
                // access allowed if the user has at least one application role for at least one zaaktype
                hasAnyPabcApplicationRole(user)
            }
        } else {
            // PABC disabled: legacy web.xml rules on token roles
            if (isAdmin) {
                user.roles.contains(ZacApplicationRole.BEHEERDER.value)
            } else {
                user.roles.any { it in ZacApplicationRole.entries.map { e -> e.value }.toSet() }
            }
        }
    }

    private fun hasAnyPabcApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { it.isNotEmpty() }

    private fun hasAnyBeheerderApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { roles ->
            roles.any { it == ZacApplicationRole.BEHEERDER.value }
        }
}
