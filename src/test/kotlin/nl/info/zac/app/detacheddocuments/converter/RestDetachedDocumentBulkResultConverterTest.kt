/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.detacheddocuments.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFailure
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFoutcode
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailure
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkFailureReason
import nl.info.zac.document.detacheddocument.model.DetachedDocumentBulkResult

class RestDetachedDocumentBulkResultConverterTest : BehaviorSpec({
    given("a bulk result with a processed document and a failure for every failure reason") {
        val detachedDocumentBulkResult = DetachedDocumentBulkResult(
            processedIds = listOf(1L),
            failures = listOf(
                DetachedDocumentBulkFailure(2L, null, DetachedDocumentBulkFailureReason.NOT_FOUND),
                DetachedDocumentBulkFailure(3L, "fakeTitel3", DetachedDocumentBulkFailureReason.LOCKED),
                DetachedDocumentBulkFailure(4L, "fakeTitel4", DetachedDocumentBulkFailureReason.ALREADY_LINKED),
                DetachedDocumentBulkFailure(5L, "fakeTitel5", DetachedDocumentBulkFailureReason.NO_RIGHTS),
                DetachedDocumentBulkFailure(6L, "fakeTitel6", DetachedDocumentBulkFailureReason.OPEN_ZAAK_ERROR),
                DetachedDocumentBulkFailure(7L, "fakeTitel7", DetachedDocumentBulkFailureReason.OPEN_ZAAK_UNAVAILABLE),
                DetachedDocumentBulkFailure(8L, "fakeTitel8", DetachedDocumentBulkFailureReason.MISSING_IN_OPEN_ZAAK)
            )
        )

        `when`("it is converted to the REST result") {
            val restDetachedDocumentBulkResult = detachedDocumentBulkResult.toRestDetachedDocumentBulkResult()

            then("the processed ids are kept and every failure reason gets its REST error code") {
                restDetachedDocumentBulkResult.verwerkt shouldBe listOf(1L)
                restDetachedDocumentBulkResult.mislukt shouldBe listOf(
                    RestDetachedDocumentBulkFailure(2L, null, RestDetachedDocumentBulkFoutcode.NIET_GEVONDEN),
                    RestDetachedDocumentBulkFailure(3L, "fakeTitel3", RestDetachedDocumentBulkFoutcode.VERGRENDELD),
                    RestDetachedDocumentBulkFailure(4L, "fakeTitel4", RestDetachedDocumentBulkFoutcode.REEDS_GEKOPPELD),
                    RestDetachedDocumentBulkFailure(5L, "fakeTitel5", RestDetachedDocumentBulkFoutcode.GEEN_RECHT),
                    RestDetachedDocumentBulkFailure(6L, "fakeTitel6", RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT),
                    RestDetachedDocumentBulkFailure(
                        7L,
                        "fakeTitel7",
                        RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_NIET_BESCHIKBAAR
                    ),
                    RestDetachedDocumentBulkFailure(
                        8L,
                        "fakeTitel8",
                        RestDetachedDocumentBulkFoutcode.ONTBREEKT_IN_OPEN_ZAAK
                    )
                )
            }
        }
    }
})
