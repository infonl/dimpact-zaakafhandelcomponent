/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkRequest.Companion.MAX_IDS
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestDetachedDocumentBulkLinkRequest(
    @field:NotEmpty
    @field:Size(max = MAX_IDS)
    var ids: List<Long>,

    @field:NotBlank
    var nieuweZaakID: String
)
