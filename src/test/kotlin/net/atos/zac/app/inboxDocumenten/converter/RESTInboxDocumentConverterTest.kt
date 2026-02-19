/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.inboxDocumenten.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import net.atos.zac.app.inboxdocumenten.converter.RESTInboxDocumentConverter
import net.atos.zac.documenten.model.InboxDocument
import nl.info.zac.model.createInboxDocument
import java.time.LocalDate
import java.util.UUID

class RESTInboxDocumentConverterTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Single document conversion") {
        Given("A valid inbox document with all fields populated") {
            val documentId = 123L
            val documentUUID = UUID.randomUUID()
            val documentID = "DOC-2025-001"
            val titel = "Important Document"
            val creatiedatum = LocalDate.of(2025, 2, 15)
            val bestandsnaam = "document.pdf"
            val informatieobjectTypeUUID = UUID.randomUUID()

            val inboxDocument = createInboxDocument(
                uuid = documentUUID,
                id = documentId,
                enkelvoudiginformatieobjectID = documentID,
                titel = titel,
                creatiedatum = creatiedatum,
                bestandsnaam = bestandsnaam
            )

            When("convert is called with the document and informatieobjecttype UUID") {
                val result = RESTInboxDocumentConverter.convert(inboxDocument, informatieobjectTypeUUID)

                Then("all fields should be correctly mapped") {
                    result.id shouldBe documentId
                    result.enkelvoudiginformatieobjectUUID shouldBe documentUUID
                    result.enkelvoudiginformatieobjectID shouldBe documentID
                    result.informatieobjectTypeUUID shouldBe informatieobjectTypeUUID
                    result.titel shouldBe titel
                    result.creatiedatum shouldBe creatiedatum
                    result.bestandsnaam shouldBe bestandsnaam
                }
            }
        }

        Given("An inbox document with minimal data") {
            val inboxDocument = createInboxDocument()
            val informatieobjectTypeUUID = UUID.randomUUID()

            When("convert is called") {
                val result = RESTInboxDocumentConverter.convert(inboxDocument, informatieobjectTypeUUID)

                Then("all fields from the source document should be present in the result") {
                    result.id shouldBe inboxDocument.id
                    result.enkelvoudiginformatieobjectUUID shouldBe inboxDocument.enkelvoudiginformatieobjectUUID
                    result.enkelvoudiginformatieobjectID shouldBe inboxDocument.enkelvoudiginformatieobjectID
                    result.informatieobjectTypeUUID shouldBe informatieobjectTypeUUID
                    result.titel shouldBe inboxDocument.titel
                    result.creatiedatum shouldBe inboxDocument.creatiedatum
                    result.bestandsnaam shouldBe inboxDocument.bestandsnaam
                }
            }
        }

        Given("Different informatieobjecttype UUIDs") {
            val inboxDocument = createInboxDocument()
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()

            When("convert is called with different UUIDs") {
                val result1 = RESTInboxDocumentConverter.convert(inboxDocument, uuid1)
                val result2 = RESTInboxDocumentConverter.convert(inboxDocument, uuid2)

                Then("each result should have the corresponding informatieobjecttype UUID") {
                    result1.informatieobjectTypeUUID shouldBe uuid1
                    result2.informatieobjectTypeUUID shouldBe uuid2
                }

                And("other fields should remain identical") {
                    result1.id shouldBe result2.id
                    result1.enkelvoudiginformatieobjectUUID shouldBe result2.enkelvoudiginformatieobjectUUID
                    result1.titel shouldBe result2.titel
                }
            }
        }

        Given("An inbox document with special characters in fields") {
            val specialTitel = "Document: Test & Validation <2025>"
            val specialBestandsnaam = "file-name_with.special#chars.pdf"
            val inboxDocument = createInboxDocument(
                titel = specialTitel,
                bestandsnaam = specialBestandsnaam
            )
            val informatieobjectTypeUUID = UUID.randomUUID()

            When("convert is called") {
                val result = RESTInboxDocumentConverter.convert(inboxDocument, informatieobjectTypeUUID)

                Then("special characters should be preserved") {
                    result.titel shouldBe specialTitel
                    result.bestandsnaam shouldBe specialBestandsnaam
                }
            }
        }
    }

    Context("List conversion with valid UUIDs") {
        Given("Multiple inbox documents with corresponding informatieobjecttype UUIDs") {
            val doc1 = createInboxDocument(
                UUID.randomUUID(),
                id = 1L
            )
            val doc2 = createInboxDocument(
                UUID.randomUUID(),
                id = 2L
            )
            val doc3 = createInboxDocument(
                UUID.randomUUID(),
                id = 3L
            )

            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()
            val uuid3 = UUID.randomUUID()

            val documents = listOf(doc1, doc2, doc3)
            val uuids = listOf(uuid1, uuid2, uuid3)

            When("convert is called with the lists") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("all documents should be converted") {
                    results shouldHaveSize 3
                }

                And("each result should have the correct mapping") {
                    results[0]?.id shouldBe 1L
                    results[0]?.informatieobjectTypeUUID shouldBe uuid1

                    results[1]?.id shouldBe 2L
                    results[1]?.informatieobjectTypeUUID shouldBe uuid2

                    results[2]?.id shouldBe 3L
                    results[2]?.informatieobjectTypeUUID shouldBe uuid3
                }
            }
        }

        Given("An empty list of documents") {
            val documents = emptyList<InboxDocument>()
            val uuids = emptyList<UUID>()

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("the result should be an empty list") {
                    results.shouldBeEmpty()
                }
            }
        }

        Given("A single document in a list") {
            val document = createInboxDocument()
            val uuid = UUID.randomUUID()

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(listOf(document), listOf(uuid))

                Then("the result should contain one converted document") {
                    results shouldHaveSize 1
                    results[0]?.id shouldBe document.id
                    results[0]?.informatieobjectTypeUUID shouldBe uuid
                }
            }
        }
    }

    Context("List conversion with null UUIDs") {
        Given("Documents where some informatieobjecttype UUIDs are null") {
            val doc1 = createInboxDocument(
                id = 1L,
                titel = "Doc1"
            )
            val doc2 = createInboxDocument(
                id = 2L,
                titel = "Doc2"
            )
            val doc3 = createInboxDocument(
                id = 3L,
                titel = "Doc3"
            )
            val doc4 = createInboxDocument(
                id = 4L,
                titel = "Doc4"
            )

            val uuid1 = UUID.randomUUID()
            val uuid3 = UUID.randomUUID()

            val documents = listOf(doc1, doc2, doc3, doc4)
            val uuids = listOf(uuid1, null, uuid3, null)

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("only documents with non-null UUIDs should be included") {
                    results shouldHaveSize 2
                }

                And("the correct documents should be converted") {
                    results.map { it?.id } shouldContainExactly listOf(1L, 3L)
                    results.map { it?.titel } shouldContainExactly listOf("Doc1", "Doc3")
                    results[0]?.informatieobjectTypeUUID shouldBe uuid1
                    results[1]?.informatieobjectTypeUUID shouldBe uuid3
                }
            }
        }

        Given("All informatieobjecttype UUIDs are null") {
            val doc1 = createInboxDocument(
                id = 1L
            )
            val doc2 = createInboxDocument(
                id = 2L
            )

            val documents = listOf(doc1, doc2)
            val uuids = listOf<UUID?>(null, null)

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("the result should be an empty list") {
                    results.shouldBeEmpty()
                }
            }
        }

        Given("First and last UUIDs are null") {
            val doc1 = createInboxDocument(
                id = 1L
            )
            val doc2 = createInboxDocument(
                id = 2L
            )
            val doc3 = createInboxDocument(
                id = 3L
            )

            val uuid2 = UUID.randomUUID()

            val documents = listOf(doc1, doc2, doc3)
            val uuids = listOf(null, uuid2, null)

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("only the middle document should be converted") {
                    results shouldHaveSize 1
                    results[0]?.id shouldBe 2L
                    results[0]?.informatieobjectTypeUUID shouldBe uuid2
                }
            }
        }
    }

    Context("List conversion edge cases") {
        Given("Many documents with alternating null and valid UUIDs") {
            val documents = (1..10).map { i ->
                createInboxDocument(
                    id = i.toLong(),
                    titel = "Document $i"
                )
            }
            val uuids = (1..10).map { i ->
                if (i % 2 == 0) UUID.randomUUID() else null
            }

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("only even-indexed documents should be converted") {
                    results shouldHaveSize 5
                    results.map { it?.id } shouldContainExactly listOf(2L, 4L, 6L, 8L, 10L)
                }
            }
        }

        Given("Documents with different date values") {
            val date1 = LocalDate.of(2025, 1, 1)
            val date2 = LocalDate.of(2025, 6, 15)
            val date3 = LocalDate.of(2025, 12, 31)

            val doc1 = createInboxDocument(
                creatiedatum = date1
            )
            val doc2 = createInboxDocument(
                creatiedatum = date2
            )
            val doc3 = createInboxDocument(
                creatiedatum = date3
            )

            val uuid = UUID.randomUUID()

            val documents = listOf(doc1, doc2, doc3)
            val uuids = listOf(uuid, uuid, uuid)

            When("convert is called") {
                val results = RESTInboxDocumentConverter.convert(documents, uuids)

                Then("all dates should be correctly preserved") {
                    results[0]?.creatiedatum shouldBe date1
                    results[1]?.creatiedatum shouldBe date2
                    results[2]?.creatiedatum shouldBe date3
                }
            }
        }
    }

    Context("Consistency between single and list conversion") {
        Given("The same document") {
            val document = createInboxDocument()
            val informatieobjectTypeUUID = UUID.randomUUID()

            When("converting as single and in a list") {
                val singleResult = RESTInboxDocumentConverter.convert(document, informatieobjectTypeUUID)
                val listResult = RESTInboxDocumentConverter.convert(
                    listOf(document),
                    listOf(informatieobjectTypeUUID)
                )

                Then("both results should be identical") {
                    listResult shouldHaveSize 1

                    listResult[0]?.id shouldBe singleResult.id
                    listResult[0]?.enkelvoudiginformatieobjectUUID shouldBe singleResult.enkelvoudiginformatieobjectUUID
                    listResult[0]?.enkelvoudiginformatieobjectID shouldBe singleResult.enkelvoudiginformatieobjectID
                    listResult[0]?.informatieobjectTypeUUID shouldBe singleResult.informatieobjectTypeUUID
                    listResult[0]?.titel shouldBe singleResult.titel
                    listResult[0]?.creatiedatum shouldBe singleResult.creatiedatum
                    listResult[0]?.bestandsnaam shouldBe singleResult.bestandsnaam
                }
            }
        }

        Given("Multiple conversions of the same document with different UUIDs") {
            val document = createInboxDocument()
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()
            val uuid3 = UUID.randomUUID()

            When("converting multiple times") {
                val singleResults = listOf(
                    RESTInboxDocumentConverter.convert(document, uuid1),
                    RESTInboxDocumentConverter.convert(document, uuid2),
                    RESTInboxDocumentConverter.convert(document, uuid3)
                )
                val listResults = RESTInboxDocumentConverter.convert(
                    listOf(document, document, document),
                    listOf(uuid1, uuid2, uuid3)
                )

                Then("single and list results should match") {
                    listResults shouldHaveSize 3

                    singleResults.forEachIndexed { index, singleResult ->
                        listResults[index]?.id shouldBe singleResult.id
                        listResults[index]?.informatieobjectTypeUUID shouldBe singleResult.informatieobjectTypeUUID
                        listResults[index]?.titel shouldBe singleResult.titel
                    }
                }
            }
        }
    }

    Context("Field preservation verification") {
        Given("Documents with various ID values") {
            val testIds = listOf(0L, 1L, 999L, Long.MAX_VALUE)

            testIds.forEach { idValue ->
                val document = createInboxDocument(id = idValue)
                val uuid = UUID.randomUUID()

                When("convert is called with ID $idValue") {
                    val result = RESTInboxDocumentConverter.convert(document, uuid)

                    Then("the ID should be preserved exactly") {
                        result.id shouldBe idValue
                    }
                }
            }
        }

        Given("Documents with various string field values") {
            val testCases = listOf(
                Triple("Short", "a.pdf", "ID-1"),
                Triple("", "empty.txt", ""),
                Triple("Very Long Title " + "x".repeat(1000), "long-filename.doc", "LONG-ID-" + "0".repeat(100))
            )

            testCases.forEach { (titel, bestandsnaam, documentID) ->
                val document = createInboxDocument(
                    titel = titel,
                    bestandsnaam = bestandsnaam,
                    enkelvoudiginformatieobjectID = documentID
                )
                val uuid = UUID.randomUUID()

                When("convert is called with titel='$titel'") {
                    val result = RESTInboxDocumentConverter.convert(document, uuid)

                    Then("all string fields should be preserved exactly") {
                        result.titel shouldBe titel
                        result.bestandsnaam shouldBe bestandsnaam
                        result.enkelvoudiginformatieobjectID shouldBe documentID
                    }
                }
            }
        }
    }
})
