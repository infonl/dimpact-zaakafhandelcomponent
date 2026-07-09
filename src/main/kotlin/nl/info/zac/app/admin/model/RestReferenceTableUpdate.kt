/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTableUpdate(
    @field:NotBlank
    var naam: String,

    var code: String? = null,

    @field:Valid
    var waarden: List<RestReferenceTableValue> = emptyList()
)
