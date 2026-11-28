/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

sealed class InformatieobjectEvent(description: String, val informatieobjectUuid: UUID) : LogEvent(description) {
    class DocumentCleanUpFailed(informatieobjectUuid: UUID) :
        InformatieobjectEvent("Failed to delete document", informatieobjectUuid)

    class DocumentMissingInOpenZaak(informatieobjectUuid: UUID) :
        InformatieobjectEvent("Document wel gevonden in de database, maar niet in OpenZaak", informatieobjectUuid)

    class IncompleteDocumentDeletionStarted(informatieobjectUuid: UUID) :
        InformatieobjectEvent(
            "Deletion of a document started because uploading its content in parts failed",
            informatieobjectUuid
        )
}
