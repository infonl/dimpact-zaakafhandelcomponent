/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.test.org.keycloak.representations.idm

import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.UserRepresentation
import java.util.UUID

fun createUserRepresentation(
    id: String = UUID.randomUUID().toString(),
    username: String = "fakeUsername",
    firstName: String? = "fakeFirstName",
    lastName: String? = "fakeLastName",
    email: String = "fake@example.com"
) = UserRepresentation().apply {
    this.id = id
    this.username = username
    this.firstName = firstName
    this.lastName = lastName
    this.email = email
}

fun createGroupRepresentation(
    id: String = "fakeGroupId",
    name: String = "fakeGroupName",
    attributes: Map<String, List<String>> = mapOf("fakeKey" to listOf("fakeValue")),
    clientRoles: Map<String, List<String>> = emptyMap()
) = GroupRepresentation().apply {
    this.id = id
    this.name = name
    this.attributes = attributes
    this.clientRoles = clientRoles
}
