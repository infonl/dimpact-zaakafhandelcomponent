/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.model

import nl.info.zac.app.shared.RestResultaat
import nl.info.zac.app.identity.model.RestUser

class RestDetachedDocumentResult(
    resultaten: Collection<RestDetachedDocument>,
    aantalTotaal: Long
) : RestResultaat<RestDetachedDocument>(resultaten, aantalTotaal) {
    var filterOntkoppeldDoor: List<RestUser> = emptyList()
}
