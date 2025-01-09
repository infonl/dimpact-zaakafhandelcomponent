/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestDecisionWithdrawalData(
    @field:NotNull
    var besluitUuid: UUID,

    @field:NotBlank
    var reden: String,

    var vervaldatum: LocalDate? = null,

    @field:NotBlank
    var vervalreden: String
)
