/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import okhttp3.Headers
import org.json.JSONArray

/**
 * This test assumes previous tests completed successfully.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class MailRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaak exists and SMTP server is configured") {
        When("A mail is sent") {
            val recieverMail = "reciever@example.com"
            val body = "<p><b>bold</b>paragraph<i>italic</i></p>"

            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/mail/send/$zaak1UUID",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                ),
                requestBodyAsString = """{
                    "verzender": "sender@example.com",
                    "ontvanger": "$recieverMail",
                    "replyTo": "replyTo@example.com",
                    "onderwerp": "subject",
                    "body": "$body",
                    "bijlagen": "$enkelvoudigInformatieObjectUUID",
                    "createDocumentFromMail": false
                }
                """.trimIndent(),
                addAuthorizationHeader = true
            )

            Then("the responses should be 'ok'") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
            }

            And("received mail should contain the right details") {
                val receivedMailsResponse = itestHttpClient.performGetRequest(
                    url = "http://localhost:8888/api/user/$recieverMail/messages/"
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
                            shouldContain("Content-Type: application/text; name=testTextDocument.txt")
                            shouldContain("Content-Disposition: attachment; filename=testTextDocument.txt")
                        }
                    }
                }
            }
        }
    }
})
