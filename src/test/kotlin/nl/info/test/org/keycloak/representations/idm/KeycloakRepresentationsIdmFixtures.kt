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
    username: String = "dummyUsername",
    firstName: String? = "dummyFirstName",
    lastName: String? = "dummyLastName",
    email: String = "dummy@example.com"
) = UserRepresentation().apply {
    this.id = id
    this.username = username
    this.firstName = firstName
    this.lastName = lastName
    this.email = email
}

fun createGroupRepresentation(
    id: String = "dummyGroupId",
    name: String = "dummyGroupName",
    attributes: Map<String, List<String>> = mapOf("dummyKey" to listOf("dummyValue"))
) = GroupRepresentation().apply {
    this.id = id
    this.name = name
    this.attributes = attributes
}
