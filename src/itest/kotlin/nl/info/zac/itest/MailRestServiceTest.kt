/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.TEST_INFORMATIE_OBJECT_TYPE_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONArray
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate

/**
 * This test assumes previous tests completed successfully.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class MailRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    val urlEncodedFileName = URLEncoder.encode(TEST_TXT_FILE_NAME, Charsets.UTF_8)

    Given("A zaak exists and SMTP server is configured") {
        When("A mail is sent with the 'create document from mail' option enabled") {
            val receiverMail = "receiverMailTest@example.com"
            val body = "<p><b>bold</b>paragraph<i>italic</i></p>"

            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/mail/send/$zaakProductaanvraag1Uuid",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                ),
                requestBodyAsString = """{
                    "verzender": "sender@example.com",
                    "ontvanger": "$receiverMail",
                    "replyTo": "replyTo@example.com",
                    "onderwerp": "subject",
                    "body": "$body",
                    "bijlagen": "$enkelvoudigInformatieObjectUUID",
                    "createDocumentFromMail": true
                }
                """.trimIndent(),
                addAuthorizationHeader = true
            )

            Then("the response should be 'no-content'") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
            }

            And("the received mail should contain the right details") {
                val receivedMailsResponse = itestHttpClient.performGetRequest(
                    url = "$GREENMAIL_API_URI/user/$receiverMail/messages/"
                )
                receivedMailsResponse.code shouldBe HTTP_STATUS_OK

                val receivedMails = JSONArray(receivedMailsResponse.body!!.string())
                with(receivedMails) {
                    length() shouldBe 1
                    with(getJSONObject(0)) {
                        getString("subject") shouldBe "subject"
                        getString("contentType") shouldStartWith "multipart/mixed"
                        with(getString("mimeMessage")) {
                            shouldContain(body)
                            shouldContain("Content-Type: application/text; name*=UTF-8''$urlEncodedFileName")
                            shouldContain("Content-Disposition: attachment; filename*=UTF-8''$urlEncodedFileName")
                        }
                    }
                }
            }

            And(
                """
                a PDF document should be added to the zaak as enkelvoudiginformatieobject containing the email details
                """
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobjectenList",
                    requestBodyAsString = """
                        {
                            "zaakUUID": "$zaakProductaanvraag1Uuid",
                            "gekoppeldeZaakDocumenten": false
                        }
                    """.trimIndent()
                )
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                // the email PDF should always be the first
                JSONArray(responseBody)[0].toString() shouldEqualJsonIgnoringExtraneousFields """
                {
                  "bestandsnaam" : "subject.pdf",
                  "auteur" : "$TEST_USER_1_NAME",
                  "beschrijving" : "",
                  "bestandsomvang" : 1851,
                  "creatiedatum" : "${LocalDate.now()}",
                  "formaat" : "application/pdf",
                  "indicatieGebruiksrecht" : false,
                  "indicaties" : [ "VERZONDEN" ],
                  "informatieobjectTypeOmschrijving" : "e-mail",
                  "informatieobjectTypeUUID" : "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                  "isBesluitDocument" : false,
                  "link" : "",
                  "rechten" : {
                    "lezen" : true,
                    "ondertekenen" : true,
                    "ontgrendelen" : true,
                    "toevoegenNieuweVersie" : true,
                    "vergrendelen" : true,
                    "verwijderen" : true,
                    "wijzigen" : true
                  }
                }
                """.trimIndent()
            }
        }
    }
})
