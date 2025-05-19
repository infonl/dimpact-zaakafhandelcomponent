/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.rest

import jakarta.validation.constraints.NotEmpty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestSmartDocumentsPath(
    @field:NotEmpty
    var path: List<String>
)
