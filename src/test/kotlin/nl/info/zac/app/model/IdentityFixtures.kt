/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.model

import nl.info.zac.app.identity.model.RestUser

fun createRESTUser(
    id: String = "fakeId",
    name: String = "fakeUserName"
) = RestUser(
    id = id,
    naam = name
)
