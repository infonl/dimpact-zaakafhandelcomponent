/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestDetachedDocumentBulkRequest(
    @field:NotEmpty
    @field:Size(max = MAX_IDS)
    var ids: List<Long>
) {
    companion object {
        const val MAX_IDS = 100
    }
}
