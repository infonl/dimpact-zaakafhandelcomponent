/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaakspecifiekGeautoriseerdeMedewerker(
    @field:NotBlank
    var groepId: String,

    @field:NotBlank
    var medewerkerId: String
)
