/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.log

import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.log.LogEvent

class DetachedDocumentNotProcessed(
    val detachedDocumentId: Long,
    val failureReason: DetachedDocumentBulkFailureReason
) : LogEvent("Ontkoppeld document kon niet worden verwerkt")
