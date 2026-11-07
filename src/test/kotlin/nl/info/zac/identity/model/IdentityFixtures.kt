/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.identity.model

fun createGroup(
    id: String = "dummyId",
    name: String = "dummyName",
    email: String = "dummy-group@example.com"
) = Group(
    id,
    name,
    email
)

fun createUser(
    id: String = "dummyId",
    firstName: String = "dummyFirstName",
    lastName: String = "dummyLastName",
    fullName: String = "dummyFullName",
    email: String = "dummy@example.com"
) = User(
    id,
    firstName,
    lastName,
    fullName,
    email
)
