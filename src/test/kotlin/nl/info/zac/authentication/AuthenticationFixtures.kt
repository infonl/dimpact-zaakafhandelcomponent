/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

@Suppress("LongParameterList")
fun createLoggedInUser(
    id: String = "fakeId",
    firstName: String = "fakeFirstName",
    lastName: String = "fakeLastName",
    displayName: String = "fakeDisplayName",
    email: String = "fake@example.com",
    roles: Set<String> = setOf("fakeRole1", "fakeRole2"),
    groups: Set<String> = setOf("fakeGroup1", "fakeGroup2"),
    applicationRolesPerZaaktype: Map<String, Set<String>> = emptyMap(),
    overallRoles: Set<String> = emptySet(),
    brpGemeenten: Map<String, String> = emptyMap()
) = LoggedInUser(
    id,
    firstName,
    lastName,
    displayName,
    email,
    roles,
    groups,
    applicationRolesPerZaaktype,
    overallRoles,
    brpGemeenten
)
