/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package org.keycloak.representations.idm

fun createUserRepresentation(
    username: String = "dummyUsername",
    firstName: String? = "dummyFirstName",
    lastName: String? = "dummyLastName",
    email: String = "dummy@example.com"
) = UserRepresentation().apply {
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
