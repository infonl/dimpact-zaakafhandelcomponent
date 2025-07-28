/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

data class ZaakInput(
    val loggedInUser: LoggedInUser,

    @field:JsonbProperty("zaak")
    val zaakData: ZaakData
) : UserInput(loggedInUser)
