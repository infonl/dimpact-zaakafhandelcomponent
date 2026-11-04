/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.model

data class DetachedDocumentBulkResult(
    val processedIds: List<Long>,
    val failures: List<DetachedDocumentBulkFailure>
)
