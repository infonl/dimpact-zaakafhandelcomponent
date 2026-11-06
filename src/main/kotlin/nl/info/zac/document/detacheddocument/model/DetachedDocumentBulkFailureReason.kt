/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.model

enum class DetachedDocumentBulkFailureReason {
    NOT_FOUND,
    MISSING_IN_OPEN_ZAAK,
    LOCKED,
    ALREADY_LINKED,
    NO_RIGHTS,
    OPEN_ZAAK_ERROR,
    OPEN_ZAAK_UNAVAILABLE
}
