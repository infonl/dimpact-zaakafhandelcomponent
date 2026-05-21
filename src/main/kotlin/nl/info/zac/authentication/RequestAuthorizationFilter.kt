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

/**
 * Generic ZAC authorisation filter.
 * Checks an explicit set of unauthenticated endpoints for allowed HTTP methods.
 * For authenticated endpoints, it expects that the user has already logged in and performs basic authorization.
 *
 * General access: user must have at least one application role on at least one zaaktype.
 * For admin URIs (/admin/, /rest/admin/): User must have the 'beheerder' role for at least one zaaktype
 *
 * This filter must run after [UserPrincipalFilter], so [UserPrincipalFilter] can
 * authenticate via Elytron OIDC and build the LoggedInUser in the session first.
 */
@ApplicationScoped
@WebFilter(filterName = "RequestAuthorizationFilter")
@AllOpen
@NoArgConstructor
class RequestAuthorizationFilter @Inject constructor() : Filter {
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
            // allow unauthenticated access on the following paths
            requestPath.startsWith("/webdav/") -> true
            // allow unauthenticated access, but only for specific HTTP methods on the following paths
            requestPath == "/rest/notificaties" -> httpRequestMethod == POST
            requestPath.startsWith("/rest/internal/") -> httpRequestMethod == GET || httpRequestMethod == DELETE
            requestPath == "/websocket" -> httpRequestMethod == GET
            requestPath.startsWith("/rest/document-creation/smartdocuments/callback/") -> httpRequestMethod == POST
            requestPath == "/static/smart-documents-result.html" -> httpRequestMethod == GET
            requestPath.startsWith("/assets/") || requestPath == "/logout" || requestPath == "/favicon.ico" -> httpRequestMethod == GET
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
        return if (isAdmin) {
            hasBeheerderApplicationRole(user)
        } else {
            hasAnyApplicationRole(user)
        }
    }

    /**
     * Checks if the user has at least one application role for at least one zaaktype,
     * or if the user has any overall roles.
     */
    private fun hasAnyApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { it.isNotEmpty() } || user.overallRoles.isNotEmpty()

    /**
     * Checks if the user has the 'beheerder' role for at least one zaaktype,
     * or if the user has the 'beheerder' role for at least one of the overall roles.
     */
    private fun hasBeheerderApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { roles ->
            roles.any { it == ZacApplicationRole.BEHEERDER.value }
        } || user.overallRoles.contains(ZacApplicationRole.BEHEERDER.value)
}
