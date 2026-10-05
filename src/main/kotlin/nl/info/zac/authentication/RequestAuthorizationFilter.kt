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
 * General access: user must have at least one read ('lezen') application role on at least one zaaktype,
 * or as an overall role. A user without one gets a dedicated error page, on every authenticated path.
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
        private const val NO_READ_APPLICATION_ROLE_ERROR_PAGE = "/static/error-403-no-read-role.html"
        private val PUBLIC_STATIC_PATHS = setOf(
            "/sign-out",
            "/favicon.ico",
            "/favicon.svg",
            "/apple-touch-icon.png",
            "/site.webmanifest",
        )
    }

    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain
    ) {
        val request = servletRequest as HttpServletRequest
        val response = servletResponse as HttpServletResponse
        when (authorize(request)) {
            Authorization.ALLOWED -> filterChain.doFilter(request, response)
            Authorization.NO_READ_APPLICATION_ROLE -> showNoReadApplicationRoleErrorPage(request, response)
            Authorization.FORBIDDEN -> response.sendError(HttpServletResponse.SC_FORBIDDEN)
        }
    }

    private enum class Authorization { ALLOWED, NO_READ_APPLICATION_ROLE, FORBIDDEN }

    private fun authorize(request: HttpServletRequest): Authorization {
        val requestPath = request.requestURI.removePrefix(request.contextPath)
        val httpRequestMethod = request.method
        val isAllowed = when {
            // allow unauthenticated access on the following paths
            requestPath.startsWith("/webdav/") -> true
            // allow unauthenticated access, but only for specific HTTP methods on the following paths
            requestPath == "/rest/notificaties" -> httpRequestMethod == POST
            requestPath.startsWith("/rest/internal/") -> httpRequestMethod == GET || httpRequestMethod == DELETE
            requestPath == "/websocket" -> httpRequestMethod == GET
            requestPath.startsWith("/rest/document-creation/smartdocuments/callback/") -> httpRequestMethod == POST
            requestPath == "/static/smart-documents-result.html" -> httpRequestMethod == GET
            requestPath.startsWith("/assets/") || requestPath in PUBLIC_STATIC_PATHS -> httpRequestMethod == GET
            // for all other paths, authorization is required
            else -> return authorizeUser(request, requestPath)
        }
        return if (isAllowed) Authorization.ALLOWED else Authorization.FORBIDDEN
    }

    private fun authorizeUser(request: HttpServletRequest, requestPath: String): Authorization {
        val user = request.getSession(false)?.let(::getLoggedInUser) ?: return Authorization.FORBIDDEN
        return when {
            !user.hasReadApplicationRole -> Authorization.NO_READ_APPLICATION_ROLE
            ADMIN_URI_PREFIXES.any(requestPath::startsWith) && !hasBeheerderApplicationRole(user) ->
                Authorization.FORBIDDEN
            else -> Authorization.ALLOWED
        }
    }

    /**
     * The web.xml 403 error page is shared by every 403, so this one is forwarded to here.
     * `no-store` keeps the browser from revalidating the forwarded static page into a 304.
     */
    private fun showNoReadApplicationRoleErrorPage(request: HttpServletRequest, response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.setHeader("Cache-Control", "no-store")
        request.getRequestDispatcher(NO_READ_APPLICATION_ROLE_ERROR_PAGE).forward(request, response)
    }

    /**
     * Checks if the user has the 'beheerder' role for at least one zaaktype,
     * or if the user has the 'beheerder' role for at least one of the overall roles.
     */
    private fun hasBeheerderApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { roles ->
            roles.any { it == ZacApplicationRole.BEHEERDER.value }
        } || user.overallRoles.contains(ZacApplicationRole.BEHEERDER.value)
}
