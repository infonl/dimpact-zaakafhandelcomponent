/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.identity.model

fun createRESTUser(
    id: String = "dummyId",
    name: String = "dummyUserName"
) = RestUser(
    id = id,
    naam = name
)
