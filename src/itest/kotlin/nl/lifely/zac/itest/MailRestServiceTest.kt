/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.lifely.zac.itest.config.ItestConfiguration.SMTP_SERVER_PORT
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_COMPLETED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import okhttp3.Headers

/**
 * This test assumes previous tests completed successfully.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_COMPLETED)
class MailRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val greenMail = GreenMail(ServerSetup(SMTP_SERVER_PORT, null, ServerSetup.PROTOCOL_SMTPS))
        .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication())

    beforeSpec() {
        greenMail.start();
    }

    afterSpec() {
        greenMail.stop();
    }

    Given("A zaak exists and SMTP server is configured") {
        When("A mail is sent") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/mail/send/$zaak1UUID",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                ),
                requestBodyAsString = """{
                    "verzender": "sender@example.com",
                    "ontvanger": "reciever@example.com",
                    "replyTo": "replyTo@example.com",
                    "onderwerp": "subject",
                    "body": "<p><b>bold</b>paragraph<i>italic</i></p>",
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

            And("received mail contains the right details") {
                val emails = greenMail.receivedMessages
                emails.size shouldBe 1
                with(emails[0]) {
                    subject shouldBe "subject"
                    content shouldBe "<p><b>bold</b>paragraph<i>italic</i></p>"
                }
           }
        }
    }
})
