/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import nl.info.zac.app.shared.RestResultaat
import nl.info.zac.app.identity.model.RestUser

class RestDetachedDocumentResult(
    results: Collection<RestDetachedDocument>,
    resultCount: Long
) : RestResultaat<RestDetachedDocument>(results, resultCount) {
    var filterOntkoppeldDoor: List<RestUser> = emptyList()
}
