/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.COORDINATOR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGER_DOMAIN_TEST_1
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

class DetachedDocumentRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)

    Given("A zaak exists with a document attached to it") {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_TEST_2_UUID,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )
        val documentTitle = "detachedDocumentItestTitle-${System.currentTimeMillis()}"
        val (documentUuid, _) = documentHelper.uploadDocumentToZaak(
            zaakUuid = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            documentTitle = documentTitle,
            authorName = FAKE_AUTHOR_NAME,
            mediaType = PDF_MIME_TYPE,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )
        val detachReason = "fakeDetachReason"

        When("the detach document endpoint is called to unlink the document from the zaak") {
            val detachResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaken/zaakinformatieobjecten/ontkoppel",
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUUID" to zaakUuid.toString(),
                        "documentUUID" to documentUuid.toString(),
                        "reden" to detachReason
                    )
                ).toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            logger.info { "Detach response: ${detachResponse.bodyAsString}" }

            Then("the response should be successful (no content)") {
                detachResponse.code shouldBe HTTP_NO_CONTENT
            }

            When("the list detached documents endpoint is called by a coordinator") {
                val listResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/ontkoppeldedocumenten",
                    requestBodyAsString = """
                        {
                            "page": 0,
                            "maxResults": 25,
                            "sort": "",
                            "order": ""
                        }
                    """.trimIndent(),
                    testUser = COORDINATOR_DOMAIN_TEST_1
                )
                val listResponseBody = listResponse.bodyAsString
                logger.info { "List detached documents response: $listResponseBody" }
                val responseJson = JSONObject(listResponseBody)
                val resultaten = responseJson.getJSONArray("resultaten")
                val detachedDoc = (0 until resultaten.length())
                    .map { resultaten.getJSONObject(it) }
                    .firstOrNull { it.getString("documentUUID") == documentUuid.toString() }
                val detachedDocumentId = detachedDoc?.getLong("id")

                Then("the response should contain the detached document with the correct fields") {
                    listResponse.code shouldBe HTTP_OK
                    responseJson.getInt("totaal") shouldBeGreaterThanOrEqual 1
                    detachedDoc shouldNotBe null
                    with(detachedDoc!!.toString()) {
                        shouldContainJsonKeyValue("zaakID", zaakIdentificatie)
                        shouldContainJsonKeyValue("reden", detachReason)
                    }
                }

                When(
                    "the delete detached document endpoint is called by a recordmanager with the id of the detached document"
                ) {
                    val deleteResponse = itestHttpClient.performDeleteRequest(
                        url = "$ZAC_API_URI/ontkoppeldedocumenten/$detachedDocumentId",
                        testUser = RECORDMANAGER_DOMAIN_TEST_1
                    )
                    logger.info { "Delete response: ${deleteResponse.bodyAsString}" }

                    Then("the response should be successful (no content)") {
                        deleteResponse.code shouldBe HTTP_NO_CONTENT
                    }

                    When("the list detached documents endpoint is called again by a coordinator") {
                        val listAfterDeleteResponse = itestHttpClient.performPutRequest(
                            url = "$ZAC_API_URI/ontkoppeldedocumenten",
                            requestBodyAsString = """
                                {
                                    "page": 0,
                                    "maxResults": 25,
                                    "sort": "",
                                    "order": ""
                                }
                            """.trimIndent(),
                            testUser = COORDINATOR_DOMAIN_TEST_1
                        )
                        val listAfterDeleteBody = listAfterDeleteResponse.bodyAsString
                        logger.info { "List after delete response: $listAfterDeleteBody" }
                        val resultatenAfterDelete = JSONObject(listAfterDeleteBody).getJSONArray("resultaten")

                        Then("the previously detached document should no longer be present") {
                            listAfterDeleteResponse.code shouldBe HTTP_OK
                            val stillPresent = (0 until resultatenAfterDelete.length())
                                .map { resultatenAfterDelete.getJSONObject(it) }
                                .any { it.getString("documentUUID") == documentUuid.toString() }
                            stillPresent shouldBe false
                        }
                    }

                    When(
                        "the get enkelvoudig informatie object endpoint is called for the deleted document"
                    ) {
                        val getResponse = itestHttpClient.performGetRequest(
                            url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid/",
                            testUser = RECORDMANAGER_DOMAIN_TEST_1
                        )
                        Then(
                            "the response should be not found confirming the enkelvoudiginformatieobject was also deleted from Open Zaak"
                        ) {
                            logger.info { "Get enkelvoudiginformatieobject after delete response: ${getResponse.bodyAsString}" }
                            getResponse.code shouldBe HTTP_NOT_FOUND
                        }
                    }
                }
            }
        }
    }
})
