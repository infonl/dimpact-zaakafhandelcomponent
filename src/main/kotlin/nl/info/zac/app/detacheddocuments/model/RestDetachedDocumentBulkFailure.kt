/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestDetachedDocumentBulkFailure(
    var id: Long,
    var titel: String?,
    var foutcode: RestDetachedDocumentBulkFoutcode
)
