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
     * Application roles per zaaktype that the user is authorized for.
     * When the PABC feature is enabled, this maps zaaktypes to sets of application roles (PABC).
     */
    val applicationRolesPerZaaktype: Map<String, Set<String>> = emptyMap(),

    /**
     * Application roles that are not tied to a specific entity type.
     * In PABC, these are roles returned without an entity type and therefore apply across all entity types.
     */
    val overallRoles: Set<String> = emptySet(),

    val brpGemeentes: Map<String, String> = emptyMap(),

) : User(id, firstName, lastName, displayName, email)
