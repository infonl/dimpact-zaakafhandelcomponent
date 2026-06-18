/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

data class BrpInput(
    val loggedInUser: LoggedInUser,
    @field:JsonbProperty("gemeente_code")
    val gemeenteCode: String?
) : UserInput(loggedInUser)
