/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.inboxDocumenten.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentConverter
import nl.info.zac.model.createInboxDocument
import java.util.UUID

class RESTInboxDocumentConverterTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a valid Inbox Document") {
        val document = createInboxDocument()

        val informatieobjectTypeUUID = UUID.randomUUID()

        When("RESTInboxDocumentConverter method convert is invoked") {
            val result = RESTInboxDocumentConverter.convert(document, informatieobjectTypeUUID)

            Then("the resulting RESTInboxDocument should end up with the same values") {
                result.id shouldBe document.id
                result.enkelvoudiginformatieobjectUUID shouldBe document.enkelvoudiginformatieobjectUUID
                result.enkelvoudiginformatieobjectID shouldBe document.enkelvoudiginformatieobjectID
                result.titel shouldBe document.titel
                result.creatiedatum shouldBe document.creatiedatum
                result.bestandsnaam shouldBe document.bestandsnaam
                result.informatieobjectTypeUUID shouldBe informatieobjectTypeUUID
            }
        }
    }
})
