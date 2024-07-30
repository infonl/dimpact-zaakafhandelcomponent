/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTableUpdate(
    @field:NotBlank
    var naam: String,

    @field:NotBlank
    var code: String,

    @field:Valid
    var waarden: List<RestReferenceTableValue> = emptyList()
)
