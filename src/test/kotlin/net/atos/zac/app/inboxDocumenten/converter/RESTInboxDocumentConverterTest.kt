/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.inboxDocumenten.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentConverter
import net.atos.zac.documenten.model.InboxDocument
import java.time.LocalDate
import java.util.UUID

class RESTInboxDocumentConverterTest : BehaviorSpec({

    Given("an InboxDocument and a UUID") {
        val document = InboxDocument().apply {
            id = 1L
            enkelvoudiginformatieobjectUUID = UUID.randomUUID()
            enkelvoudiginformatieobjectID = "DOC-123"
            titel = "Test Titel"
            creatiedatum = LocalDate.now()
            bestandsnaam = "test.pdf"
        }

        val informatieobjectTypeUUID = UUID.randomUUID()

        When("convert is called") {
            val result = RESTInboxDocumentConverter.convert(document, informatieobjectTypeUUID)

            Then("it should convert to a RESTInboxDocument with the same values") {
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

    Given("a list of InboxDocuments and corresponding UUIDs") {
        val docs = List(3) { index ->
            InboxDocument().apply {
                id = index.toLong()
                enkelvoudiginformatieobjectUUID = UUID.randomUUID()
                enkelvoudiginformatieobjectID = "DOC-$index"
                titel = "Titel $index"
                creatiedatum = LocalDate.now().minusDays(index.toLong())
                bestandsnaam = "file$index.pdf"
            }
        }

        val uuids = List(3) { UUID.randomUUID() }

        When("convert list is called") {
            val resultList = RESTInboxDocumentConverter.convert(docs, uuids)

            Then("it should return a list of converted RESTInboxDocuments") {
                resultList.size shouldBe 3

                resultList.forEachIndexed { i, result ->
                    result.id shouldBe docs[i].id
                    result.enkelvoudiginformatieobjectUUID shouldBe docs[i].enkelvoudiginformatieobjectUUID
                    result.enkelvoudiginformatieobjectID shouldBe docs[i].enkelvoudiginformatieobjectID
                    result.titel shouldBe docs[i].titel
                    result.creatiedatum shouldBe docs[i].creatiedatum
                    result.bestandsnaam shouldBe docs[i].bestandsnaam
                    result.informatieobjectTypeUUID shouldBe uuids[i]
                }
            }
        }
    }
})
