/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import nl.info.zac.app.admin.model.RestReferenceTable.Companion.CODE_MAX_LENGTH
import nl.info.zac.app.admin.model.RestReferenceTable.Companion.NAAM_MAX_LENGTH
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTableUpdate(
    @field:NotBlank
    @field:Size(max = NAAM_MAX_LENGTH)
    var naam: String,

    @field:Size(max = CODE_MAX_LENGTH)
    var code: String? = null,

    @field:Valid
    var waarden: List<RestReferenceTableValue> = emptyList()
)
