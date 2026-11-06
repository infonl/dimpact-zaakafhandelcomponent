/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.converter

import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFailure
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFoutcode
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkResult
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailure
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkResult

fun DetachedDocumentBulkResult.toRestDetachedDocumentBulkResult() = RestDetachedDocumentBulkResult(
    verwerkt = processedIds,
    mislukt = failures.map { it.toRestDetachedDocumentBulkFailure() }
)

private fun DetachedDocumentBulkFailure.toRestDetachedDocumentBulkFailure() = RestDetachedDocumentBulkFailure(
    id = id,
    titel = title,
    foutcode = reason.toRestDetachedDocumentBulkFoutcode()
)

private fun DetachedDocumentBulkFailureReason.toRestDetachedDocumentBulkFoutcode() = when (this) {
    DetachedDocumentBulkFailureReason.NOT_FOUND -> RestDetachedDocumentBulkFoutcode.NIET_GEVONDEN
    DetachedDocumentBulkFailureReason.MISSING_IN_OPEN_ZAAK -> RestDetachedDocumentBulkFoutcode.ONTBREEKT_IN_OPEN_ZAAK
    DetachedDocumentBulkFailureReason.LOCKED -> RestDetachedDocumentBulkFoutcode.VERGRENDELD
    DetachedDocumentBulkFailureReason.ALREADY_LINKED -> RestDetachedDocumentBulkFoutcode.REEDS_GEKOPPELD
    DetachedDocumentBulkFailureReason.NO_RIGHTS -> RestDetachedDocumentBulkFoutcode.GEEN_RECHT
    DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR -> RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT
    DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE -> RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_NIET_BESCHIKBAAR
}
