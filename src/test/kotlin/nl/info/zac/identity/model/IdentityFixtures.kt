/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.identity.model

fun createGroup(
    id: String = "fakeId",
    name: String = "fakeName",
    email: String = "fake-group@example.com",
    zacClientRoles: List<String> = listOf("fakeDomein")
) = Group(
    id = id,
    name = name,
    email = email,
    zacClientRoles = zacClientRoles
)

fun createUser(
    id: String = "fakeId",
    firstName: String = "fakeFirstName",
    lastName: String = "fakeLastName",
    fullName: String = "fakeFullName",
    email: String = "fake@example.com"
) = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    displayName = fullName,
    email = email
)
