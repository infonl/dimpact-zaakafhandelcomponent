/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.COORDINATOR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber")
class InboxDocumentRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val maxResults = 10
    val zacClient = ZacClient(itestHttpClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val documentHelper = DocumentHelper(zacClient)

    Given("a document is created directly in Open Zaak without being linked to a zaak") {
        val uniqueTitle = "inbox-doc-itest-${UUID.randomUUID()}"
        val createResponse = openZaakClient.createEnkelvoudigInformatieobject(
            fileName = TEST_PDF_FILE_NAME,
            title = uniqueTitle
        )
        logger.info { "createEnkelvoudigInformatieobject response: ${createResponse.bodyAsString}" }
        createResponse.code shouldBe HTTP_CREATED
        val documentUuid = JSONObject(createResponse.bodyAsString).getString("url")
            .substringAfterLast("/").run(UUID::fromString)

        When("a create notification is sent to ZAC for that document") {
            documentHelper.sendEnkelvoudigInformatieobjectCreateNotification(documentUuid)

            Then("the document should appear in the inbox documents list") {
                eventually(10.seconds) {
                    val listResponse = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/inboxdocumenten",
                        requestBodyAsString = JSONObject(
                            mapOf(
                                "page" to 0,
                                "maxResults" to maxResults,
                                "titel" to uniqueTitle
                            )
                        ).toString(),
                        testUser = COORDINATOR_DOMAIN_TEST_1
                    )
                    listResponse.code shouldBe HTTP_OK
                    with(JSONObject(listResponse.bodyAsString)) {
                        getInt("totaal") shouldBe 1
                        getJSONArray("resultaten").getJSONObject(0).apply {
                            getString("enkelvoudiginformatieobjectUUID") shouldBe documentUuid.toString()
                            getString("titel") shouldBe uniqueTitle
                        }
                    }
                }
            }
        }
    }

    Given("a document is created in the document register and appears in the inbox documents list") {
        val uniqueTitle = "inbox-doc-delete-itest-${UUID.randomUUID()}"
        val createResponse = openZaakClient.createEnkelvoudigInformatieobject(
            fileName = TEST_PDF_FILE_NAME,
            title = uniqueTitle
        )
        logger.info { "createEnkelvoudigInformatieobject response: ${createResponse.bodyAsString}" }
        createResponse.code shouldBe HTTP_CREATED
        val documentUuid = JSONObject(createResponse.bodyAsString).getString("url")
            .substringAfterLast("/").run(UUID::fromString)
        documentHelper.sendEnkelvoudigInformatieobjectCreateNotification(documentUuid)
        var inboxDocumentId = 0L
        eventually(10.seconds) {
            val listResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/inboxdocumenten",
                requestBodyAsString = JSONObject(
                    mapOf(
                        "page" to 0,
                        "maxResults" to maxResults,
                        "titel" to uniqueTitle
                    )
                ).toString(),
                testUser = COORDINATOR_DOMAIN_TEST_1
            )
            listResponse.code shouldBe HTTP_OK
            JSONObject(listResponse.bodyAsString).run {
                getInt("totaal") shouldBe 1
                inboxDocumentId = getJSONArray("resultaten").getJSONObject(0).getLong("id")
            }
        }

        When("the inbox document is deleted") {
            val deleteResponse = itestHttpClient.performDeleteRequest(
                url = "$ZAC_API_URI/inboxdocumenten/$inboxDocumentId",
                testUser = COORDINATOR_DOMAIN_TEST_1
            )

            Then("the inbox document should no longer appear in the inbox documents list") {
                deleteResponse.code shouldBe HTTP_NO_CONTENT
                val listResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/inboxdocumenten",
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "page" to 0,
                            "maxResults" to maxResults,
                            "titel" to uniqueTitle
                        )
                    ).toString(),
                    testUser = COORDINATOR_DOMAIN_TEST_1
                )
                listResponse.code shouldBe HTTP_OK
                JSONObject(listResponse.bodyAsString).getInt("totaal") shouldBe 0
            }

            When("the get enkelvoudig informatie object endpoint is called for the deleted document") {
                val getResponse = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobject/$documentUuid/",
                    testUser = COORDINATOR_DOMAIN_TEST_1
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
})
