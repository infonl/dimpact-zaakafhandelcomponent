/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

import nl.info.zac.app.zaak.model.ZAAK_TYPE_1_OMSCHRIJVING
import nl.info.zac.app.zaak.model.ZAAK_TYPE_2_OMSCHRIJVING

@Suppress("LongParameterList")
fun createLoggedInUser(
    id: String = "fakeId",
    firstName: String = "fakeFirstName",
    lastName: String = "fakeLastName",
    displayName: String = "fakeDisplayName",
    email: String = "fake@example.com",
    roles: Set<String> = setOf("fakeRole1", "fakeRole2"),
    groups: Set<String> = setOf("fakeGroup1", "fakeGroup2"),
    zaakTypes: Set<String> = setOf(ZAAK_TYPE_1_OMSCHRIJVING, ZAAK_TYPE_2_OMSCHRIJVING)
) = LoggedInUser(
    id,
    firstName,
    lastName,
    displayName,
    email,
    roles,
    groups,
    zaakTypes
)
