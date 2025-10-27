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
import nl.info.zac.identity.model.ZacApplicationRole
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
                    LOG.info(
                        "Invalidating HTTP session of user '${loggedInUser.id}' " +
                            "on context path '${servletRequest.servletContext.contextPath}'. " +
                            "Creating new session for user with user principal name '${userPrincipal.name}'."
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
            val applicationRolesResponse = pabcClientService.getApplicationRoles(functionalRoles.toList())
            val zaaktypeRolesMapping = mutableMapOf<String, MutableSet<String>>()

            applicationRolesResponse.results.forEach { response ->
                val roles = response.applicationRoles
                    .mapNotNull { it.name?.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                if (response.entityType?.type.equals("zaaktype", ignoreCase = true) &&
                    !response.entityType?.id.isNullOrBlank()
                ) {
                    zaaktypeRolesMapping.computeIfAbsent(response.entityType.id) { mutableSetOf() }.addAll(roles)
                }
            }

            val rolesForAllZaaktypen = applicationRolesResponse.results
                .filter { it.entityType == null }
                .flatMap { it.applicationRoles.mapNotNull { applicationRoleModel -> applicationRoleModel.name?.trim() } }
                .filter { it.isNotEmpty() }
                .toSet()

            if (rolesForAllZaaktypen.isNotEmpty()) {
                getAllEntityTypes().forEach { zaaktypeName ->
                    zaaktypeRolesMapping.computeIfAbsent(zaaktypeName) { mutableSetOf() }.addAll(rolesForAllZaaktypen)
                }
            }
            zaaktypeRolesMapping.mapValues { it.value.toSet() }
        } catch (ex: Exception) {
            LOG.log(Level.SEVERE, "PABC application role lookup failed", ex)
            throw PabcRuntimeException("Failed to get application roles from PABC", ex)
        }

    private fun getAllEntityTypes(): Set<String> {
        val cmmn = zaaktypeCmmnConfigurationService
            .listZaaktypeCmmnConfiguration()
            .groupBy { it.zaaktypeOmschrijving }
            .values
            .map {
                    list ->
                list.maxBy {
                        cmmnConfiguration ->
                    cmmnConfiguration.creatiedatum ?: Instant.MIN.atZone(ZoneOffset.MIN)
                }
            }
            .map { it.zaaktypeOmschrijving }
            .toSet()

        val bpmn = zaaktypeBpmnConfigurationService
            .listConfigurations()
            .map { it.zaaktypeOmschrijving }
            .toSet()

        return cmmn + bpmn
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
        if (roles.contains(ZacApplicationRole.DOMEIN_ELK_ZAAKTYPE.value)) {
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
