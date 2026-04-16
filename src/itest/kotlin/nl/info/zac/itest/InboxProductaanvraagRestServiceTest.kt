/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.config.COORDINATOR_DOMAIN_TEST_1
import nl.info.zac.itest.config.RECORDMANAGER_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.OBJECTS_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.PRODUCTAANVRAAG_TYPE_INBOX_ONLY
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber")
class InboxProductaanvraagRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val openZaakClient = OpenZaakClient(itestHttpClient)
    val maxResults = 10

    Given(
        """
            A productaanvraag object with type '$PRODUCTAANVRAAG_TYPE_INBOX_ONLY' exists in Objecten,
            and that type is not mapped to any zaaktype configuration in ZAC
        """.trimIndent()
    ) {
        When("a create notification is sent to ZAC for that productaanvraag") {
            val notificationResponse = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notificaties",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json",
                    "Authorization",
                    OPEN_NOTIFICATIONS_API_SECRET_KEY
                ),
                requestBodyAsString = JSONObject(
                    mapOf(
                        "kanaal" to "objecten",
                        "resource" to "object",
                        "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_1_UUID",
                        "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_1_UUID",
                        "actie" to "create",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                        "kenmerken" to mapOf(
                            "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                        )
                    )
                ).toString()
            )

            Then(
                "the response is no content and the inbox productaanvraag appears in the list"
            ) {
                notificationResponse.code shouldBe HTTP_NO_CONTENT

                eventually(10.seconds) {
                    val listResponse = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/inbox-productaanvragen",
                        requestBodyAsString = JSONObject(
                            mapOf(
                                "page" to 0,
                                "maxResults" to maxResults,
                                "type" to PRODUCTAANVRAAG_TYPE_INBOX_ONLY
                            )
                        ).toString(),
                        testUser = COORDINATOR_DOMAIN_TEST_1
                    )
                    logger.info { "List inbox productaanvragen response: ${listResponse.bodyAsString}" }
                    listResponse.code shouldBe HTTP_OK
                    with(JSONObject(listResponse.bodyAsString)) {
                        getInt("totaal") shouldBe 1
                        with(getJSONArray("resultaten").getJSONObject(0)) {
                            getString("type") shouldBe PRODUCTAANVRAAG_TYPE_INBOX_ONLY
                            getString("productaanvraagObjectUUID") shouldBe OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_1_UUID
                        }
                    }
                }
            }
        }
    }

    Given(
        """
            A productaanvraag inbox item exists after processing a create notification for
            object '$OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_2_UUID'
        """.trimIndent()
    ) {
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to "objecten",
                    "resource" to "object",
                    "resourceUrl" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_2_UUID",
                    "hoofdObject" to "$OBJECTS_BASE_URI/$OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_2_UUID",
                    "actie" to "create",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString(),
                    "kenmerken" to mapOf(
                        "objectType" to "$OBJECTS_BASE_URI/$OBJECTTYPE_UUID_PRODUCTAANVRAAG_DIMPACT"
                    )
                )
            ).toString()
        ).also { response ->
            logger.info { "Notification POST response code: ${response.code}" }
            response.code shouldBe HTTP_NO_CONTENT
        }

        var inboxProductaanvraagId = 0L
        eventually(10.seconds) {
            val listResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/inbox-productaanvragen",
                requestBodyAsString = JSONObject(
                    mapOf(
                        "page" to 0,
                        "maxResults" to maxResults,
                        "type" to PRODUCTAANVRAAG_TYPE_INBOX_ONLY
                    )
                ).toString(),
                testUser = COORDINATOR_DOMAIN_TEST_1
            )
            listResponse.code shouldBe HTTP_OK
            val resultaten = JSONObject(listResponse.bodyAsString).getJSONArray("resultaten")
            val item = (0 until resultaten.length())
                .map { resultaten.getJSONObject(it) }
                .firstOrNull { it.getString("productaanvraagObjectUUID") == OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_2_UUID }
                ?: error("Inbox productaanvraag for object $OBJECT_PRODUCTAANVRAAG_INBOX_ONLY_2_UUID not found")
            inboxProductaanvraagId = item.getLong("id")
        }

        When("the inbox productaanvraag is deleted") {
            val deleteResponse = itestHttpClient.performDeleteRequest(
                url = "$ZAC_API_URI/inbox-productaanvragen/$inboxProductaanvraagId",
                testUser = RECORDMANAGER_DOMAIN_TEST_1
            )

            Then("the delete succeeds and the item no longer appears in the list") {
                deleteResponse.code shouldBe HTTP_NO_CONTENT
                val listResponse = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/inbox-productaanvragen",
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "page" to 0,
                            "maxResults" to maxResults,
                            "type" to PRODUCTAANVRAAG_TYPE_INBOX_ONLY
                        )
                    ).toString(),
                    testUser = COORDINATOR_DOMAIN_TEST_1
                )
                listResponse.code shouldBe HTTP_OK
                val resultaten = JSONObject(listResponse.bodyAsString).getJSONArray("resultaten")
                val remainingIds = (0 until resultaten.length()).map { resultaten.getJSONObject(it).getLong("id") }
                (inboxProductaanvraagId in remainingIds) shouldBe false
            }
        }
    }

    Given("A PDF document exists in Open Zaak") {
        val createResponse = openZaakClient.createEnkelvoudigInformatieobject(
            fileName = TEST_PDF_FILE_NAME,
            title = "inbox-productaanvraag-pdfpreview-itest-${UUID.randomUUID()}"
        )
        logger.info { "createEnkelvoudigInformatieobject response: ${createResponse.bodyAsString}" }
        createResponse.code shouldBe HTTP_CREATED
        val documentUuid = JSONObject(createResponse.bodyAsString).getString("url")
            .substringAfterLast("/").run(UUID::fromString)

        When("the pdfPreview endpoint is called for that document") {
            val previewResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/inbox-productaanvragen/$documentUuid/pdfPreview",
                testUser = COORDINATOR_DOMAIN_TEST_1
            )

            Then("the response is 200 OK") {
                logger.info { "pdfPreview response code: ${previewResponse.code}" }
                previewResponse.code shouldBe HTTP_OK
            }
        }
    }
})
