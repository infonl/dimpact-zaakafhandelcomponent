/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import nl.info.zac.identity.model.User

@Suppress("LongParameterList")
class LoggedInUser(
    id: String,
    firstName: String?,
    lastName: String?,
    displayName: String?,
    email: String?,
    val roles: Set<String>,
    val groupIds: Set<String>,
    /**
     * List of zaaktypen for which the logged-in user is authorised, or 'null' if the user is authorised for all zaaktypen.
     */
    val geautoriseerdeZaaktypen: Set<String>? = null,

    /**
     * Application roles per zaaktype that the user is authorized for.
     * When the PABC feature is enabled, this maps zaaktypes to sets of application roles (PABC).
     */
    val applicationRolesPerZaaktype: Map<String, Set<String>> = emptyMap(),

) : User(id, firstName, lastName, displayName, email) {
    fun isAuthorisedForAllZaaktypen(): Boolean = geautoriseerdeZaaktypen == null

    /**
     * If PABC-based authorization is active, use the map of application roles per zaaktype.
     * Otherwise, legacy functional (Keycloak) role-based authorization is used.
     */
    fun isAuthorisedForZaaktype(zaaktypeOmschrijving: String, pabcIntegrationEnabled: Boolean) =
        if (pabcIntegrationEnabled) {
            applicationRolesPerZaaktype[zaaktypeOmschrijving]?.isNotEmpty() == true
        } else {
            geautoriseerdeZaaktypen?.contains(zaaktypeOmschrijving) ?: true
        }
}
