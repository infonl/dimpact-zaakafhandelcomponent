/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.repository.model

import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.ListParameters

class InboxDocumentListParameters : ListParameters() {
    var titel: String? = null

    var identificatie: String? = null

    var creatiedatum: DatumRange? = null
}
