/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import java.util.UUID

class RestDocumentVerplaatsGegevens {
    companion object {
        const val INBOX_DOCUMENTEN = "inbox-documenten"
        const val ONTKOPPELDE_DOCUMENTEN = "ontkoppelde-documenten"
    }

    var documentUUID: UUID? = null

    var bron: String? = null

    var nieuweZaakID: String? = null

    fun vanuitInboxDocumenten() = INBOX_DOCUMENTEN == bron

    fun vanuitOntkoppeldeDocumenten() = ONTKOPPELDE_DOCUMENTEN == bron
}
