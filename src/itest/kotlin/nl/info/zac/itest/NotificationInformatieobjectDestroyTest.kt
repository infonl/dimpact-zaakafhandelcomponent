/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber")
class NotificationInformatieobjectDestroyTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val maxResults = 10

    given("a document that is not linked to a zaak and appears in the inbox documents list") {
        val uniqueTitle = "informatieobject-destroy-inbox-itest-${UUID.randomUUID()}"
        val createResponse = openZaakClient.createEnkelvoudigInformatieobject(
            fileName = TEST_PDF_FILE_NAME,
            title = uniqueTitle
        )
        logger.info { "createEnkelvoudigInformatieobject response: ${createResponse.bodyAsString}" }
        createResponse.code shouldBe HTTP_CREATED
        val documentUuid = JSONObject(createResponse.bodyAsString).getString("url")
            .substringAfterLast("/").run(UUID::fromString)
        documentHelper.sendEnkelvoudigInformatieobjectCreateNotification(documentUuid)
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
                testUser = COORDINATOR_1
            )
            listResponse.code shouldBe HTTP_OK
            JSONObject(listResponse.bodyAsString).getInt("totaal") shouldBe 1
        }

        `when`("a destroy notification is sent to ZAC for that document") {
            documentHelper.sendEnkelvoudigInformatieobjectDestroyNotification(documentUuid)

            then("the document should no longer appear in the inbox documents list") {
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
                        testUser = COORDINATOR_1
                    )
                    listResponse.code shouldBe HTTP_OK
                    JSONObject(listResponse.bodyAsString).getInt("totaal") shouldBe 0
                }
            }
        }
    }

    given("a zaak with a document that has been detached from it") {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            testUser = BEHANDELAAR_1
        )
        val documentTitle = "informatieobject-destroy-detached-itest-${System.currentTimeMillis()}"
        val (documentUuid, _) = documentHelper.uploadDocumentToZaak(
            zaakUuid = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            documentTitle = documentTitle,
            authorName = FAKE_AUTHOR_NAME,
            mediaType = PDF_MIME_TYPE,
            testUser = BEHANDELAAR_1
        )
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaken/zaakinformatieobjecten/ontkoppel",
            requestBodyAsString = JSONObject(
                mapOf(
                    "zaakUUID" to zaakUuid.toString(),
                    "documentUUID" to documentUuid.toString(),
                    "reden" to "fakeDetachReason"
                )
            ).toString(),
            testUser = BEHANDELAAR_1
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
        eventually(10.seconds) {
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
                testUser = COORDINATOR_1
            )
            listResponse.code shouldBe HTTP_OK
            val resultaten = JSONObject(listResponse.bodyAsString).getJSONArray("resultaten")
            val detachedDocument = (0 until resultaten.length())
                .map { resultaten.getJSONObject(it) }
                .firstOrNull { it.getString("documentUUID") == documentUuid.toString() }
            detachedDocument shouldNotBe null
            detachedDocument?.getString("zaakID") shouldBe zaakIdentificatie
        }

        `when`("a destroy notification is sent to ZAC for that document") {
            documentHelper.sendEnkelvoudigInformatieobjectDestroyNotification(documentUuid)

            then("the document should no longer appear in the detached documents list") {
                eventually(10.seconds) {
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
                        testUser = COORDINATOR_1
                    )
                    listResponse.code shouldBe HTTP_OK
                    val resultaten = JSONObject(listResponse.bodyAsString).getJSONArray("resultaten")
                    val stillPresent = (0 until resultaten.length())
                        .map { resultaten.getJSONObject(it) }
                        .any { it.getString("documentUUID") == documentUuid.toString() }
                    stillPresent shouldBe false
                }
            }
        }
    }
})
