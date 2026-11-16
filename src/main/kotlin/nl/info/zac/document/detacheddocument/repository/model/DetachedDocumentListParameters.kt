/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository.model

import nl.info.zac.shared.model.ListParameters
import nl.info.zac.time.DateRange

class DetachedDocumentListParameters : ListParameters() {
    var titel: String? = null

    var creatiedatum: DateRange? = null

    var zaakID: String? = null

    var ontkoppeldDoor: String? = null

    var ontkoppeldOp: DateRange? = null

    var reden: String? = null
}
