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
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.exception.PabcRuntimeException
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.identity.model.ZACRole
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.wildfly.security.http.oidc.OidcPrincipal
import org.wildfly.security.http.oidc.OidcSecurityContext
import org.wildfly.security.http.oidc.RefreshableOidcSecurityContext
import java.time.Instant
import java.time.ZoneOffset
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
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val zaaktypeBpmnConfigurationService: ZaaktypeBpmnConfigurationService,
    private val pabcClientService: PabcClientService,
    @ConfigProperty(name = "FEATURE_FLAG_PABC_INTEGRATION", defaultValue = "false")
    private val pabcIntegrationEnabled: Boolean
) : Filter {
    companion object {
        private val LOG = Logger.getLogger(UserPrincipalFilter::class.java.name)
        private const val GROUP_MEMBERSHIP_CLAIM_NAME = "group_membership"
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

        if (isPublicUnauthenticated(request, response)) {
            filterChain.doFilter(request, response)
            return
        }

        servletRequest.userPrincipal?.let { userPrincipal ->
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

        if (!isAuthorizationAllowed(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return
        }

        filterChain.doFilter(servletRequest, servletResponse)
    }

    private fun isPublicUnauthenticated(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        val method = request.method.uppercase()

        if (path == "/rest/notificaties") {
            if (method == "POST") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        if (path.startsWith("/rest/internal/")) {
            if (method == "GET" || method == "DELETE") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        if (path == "/websocket") {
            if (method == "GET") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        if (path.startsWith("/webdav/")) {
            return true
        }

        if (path.startsWith("/rest/document-creation/smartdocuments/cmmn-callback/")) {
            if (method == "POST") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        if (path.startsWith("/rest/document-creation/smartdocuments/bpmn-callback/")) {
            if (method == "POST") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        if (path == "/static/smart-documents-result.html") {
            if (method == "GET") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        if (path.startsWith("/assets/")) {
            if (method == "GET") {
                return true
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        return false
    }

    @Suppress("ReturnCount")
    private fun isAuthorizationAllowed(request: HttpServletRequest): Boolean {
        val session = request.getSession(false) ?: return true
        val user = getLoggedInUser(session) ?: return true
        val path = request.requestURI.removePrefix(request.contextPath ?: "")
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
                user.roles.contains(ZACRole.BEHEERDER.value)
            } else {
                user.roles.any { it in ZACRole.entries.map { e -> e.value }.toSet() }
            }
        }
    }

    private fun hasAnyPabcApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { it.isNotEmpty() }

    private fun hasAnyBeheerderApplicationRole(user: LoggedInUser): Boolean =
        user.applicationRolesPerZaaktype.values.any { roles ->
            roles.any { it == ZACRole.BEHEERDER.value }
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
                        "functional roles: '${loggedInUser.roles}' " +
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
    private fun buildApplicationRoleMappingsFromPabc(functionalRoles: Set<String>): Map<String, Set<String>> =
        try {
            pabcClientService.getApplicationRoles(functionalRoles.toList()).results
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

    /**
     * Returns the active zaaktypen for which the user is authorised, or `null` if the user is
     * authorised for all zaaktypen.
     */
    @Deprecated(
        "In PABC-based authorization, the concept of being authorized for a zaaktype is meaningless, " +
            "since a user is always authorized for a zaaktype _for specific application roles_."
    )
    private fun getAuthorisedZaaktypen(roles: Set<String>): Set<String>? =
        if (roles.contains(ZACRole.DOMEIN_ELK_ZAAKTYPE.value)) {
            null
        } else {
            val zaaktypeCmmnConfigurationDescriptions = zaaktypeCmmnConfigurationService
                .listZaaktypeCmmnConfiguration()
                // group by zaaktype omschrijving since this is the unique identifier for a
                // zaaktype
                // (not the zaaktype uuid since that changes for every version of a
                // zaaktype)
                .groupBy { it.zaaktypeOmschrijving }
                // get the zaaktypeCmmnConfigurations with the latest creation date (= the active
                // one)
                .values
                .map { list -> list.maxBy { value -> value.creatiedatum ?: Instant.MIN.atZone(ZoneOffset.MIN) } }
                // filter out the zaaktypeCmmnConfigurations that have a domain that is equal to
                // one of the user's (domain) roles
                .filter { it.domein != null && roles.contains(it.domein) }
                .map { it.zaaktypeOmschrijving }
                .toSet()

            // Note that for BPMN zaaktypes below, we're not doing ANY filtering/authorisation, as we have no domain
            // entity. This means that ALL BPMN zaaktypes will be visible to the user.
            // This function should be removed once we have migrated to the new PABC-based IAM architecture, and the
            // code below should be replaced with proper authorisation logic for BPMN zaaktypes.
            val zaaktypeBpmnProcessDefinitionDescriptions = zaaktypeBpmnConfigurationService
                .listConfigurations()
                .map { it.zaaktypeOmschrijving }

            zaaktypeCmmnConfigurationDescriptions + zaaktypeBpmnProcessDefinitionDescriptions
        }
}
