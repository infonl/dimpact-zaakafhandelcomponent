/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

class ZaakEvent(
    val type: Type,
    val zaakUuid: UUID
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        CASE_OR_PROCESS_NOT_FOUND("Case or process not found for zaak"),
        DOCUMENT_CREATION_TOKEN_UNKNOWN("Unknown or expired document creation token; storing the document as the functionele gebruiker"),
        FLOWABLE_PROCESS_DATA_DELETED("Deleted Flowable process data for zaak"),
        MAIL_PDF_CONVERSION_FAILED("Failed to convert the sent e-mail to PDF; no zaak document was created"),
        ZAAK_OF_DELETED_ROL_NOT_FOUND("Zaak of a deleted rol no longer exists; no signaleringen are deleted"),
        ZAAKEIGENSCHAP_READ_FAILED("Failed to read zaakeigenschap; reindexing the zaak defensively")
    }
}
