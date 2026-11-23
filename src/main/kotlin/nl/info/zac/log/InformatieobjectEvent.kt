/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

class InformatieobjectEvent(
    val type: Type,
    val informatieobjectUuid: UUID
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        DOCUMENT_CLEAN_UP_FAILED("Failed to delete document"),
        DOCUMENT_MISSING_IN_OPEN_ZAAK("Document wel gevonden in de database, maar niet in OpenZaak"),
        DOCUMENT_NOT_FOUND("Document niet gevonden"),
        DOCUMENT_WITH_INCOMPLETE_CONTENT_DELETING("Deleting document because uploading its content in parts failed"),
        INBOX_DOCUMENT_NOT_FOUND("Inbox document not found")
    }
}
