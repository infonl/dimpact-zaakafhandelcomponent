/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.identity.model

fun createGroup(
    id: String = "fakeId",
    name: String = "fakeName",
    email: String = "fake-group@example.com"
) = Group(
    id,
    name,
    email
)

fun createUser(
    id: String = "fakeId",
    firstName: String = "fakeFirstName",
    lastName: String = "fakeLastName",
    fullName: String = "fakeFullName",
    email: String = "fake@example.com"
) = User(
    id,
    firstName,
    lastName,
    fullName,
    email
)
