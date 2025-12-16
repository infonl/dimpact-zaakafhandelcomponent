/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_INFORMATIE_OBJECT_TYPE_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import org.json.JSONArray
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLEncoder
import java.time.LocalDate

class MailRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val urlEncodedFileName = URLEncoder.encode(TEST_TXT_FILE_NAME, Charsets.UTF_8)
    val now = System.currentTimeMillis()
    val today = LocalDate.now()

    Given("A zaak with a document exists and the SMTP server is configured and a behandelaar is logged in") {
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
        val (_, zaakUuid) = zaakHelper.createAndIndexZaak(
            zaaktypeUuid = ZAAKTYPE_TEST_2_UUID
        )
        val (informatieobjectUuid, _) = documentHelper.uploadDocumentToZaakAndIndexDocument(
            zaakUuid = zaakUuid,
            documentTitle = "${MailRestServiceTest::class.simpleName}-1-$now",
            authorName = "fakeAuthorName",
            mediaType = TEXT_MIME_TYPE,
            fileName = TEST_TXT_FILE_NAME
        )
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)

        When("A mail is sent with the 'create document from mail' option enabled") {
            val receiverMail = "receiverMailTest@example.com"
            val body = "<p><b>bold</b>paragraph<i>italic</i></p>"

            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/mail/send/$zaakUuid",
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
                    "bijlagen": "$informatieobjectUuid",
                    "createDocumentFromMail": true
                }
                """.trimIndent(),
                addAuthorizationHeader = true
            )

            Then("the response should be 'no-content'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
            }

            And("the received mail should contain the right details") {
                val receivedMailsResponse = itestHttpClient.performGetRequest(
                    url = "$GREENMAIL_API_URI/user/$receiverMail/messages/"
                )
                receivedMailsResponse.code shouldBe HTTP_OK

                val receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                with(receivedMails) {
                    // if this same test is run multiple times while Docker Compose is kept running,
                    // there may be multiple emails, so we check that at least one email was received
                    val receivedMailsCount = length()
                    receivedMailsCount shouldBeGreaterThan 0
                    // we are only interested in the last received email
                    with(getJSONObject(receivedMailsCount - 1)) {
                        getString("subject") shouldBe "subject"
                        getString("contentType") shouldStartWith "multipart/mixed"
                        with(getString("mimeMessage")) {
                            shouldContain(body)
                            shouldContain("Content-Type: text/plain; charset=UTF-8;")
                            shouldContain("name*=UTF-8''$urlEncodedFileName")
                            shouldContain("Content-Disposition: attachment; filename*=UTF-8''$urlEncodedFileName")
                        }
                    }
                }
            }

            And(
                """
                a PDF document should be added to the zaak as enkelvoudiginformatieobject containing the email details,
                and the return permissions should be those of the logged in behandelaar
                """
            ) {
                val response = itestHttpClient.performPutRequest(
                    url = "$ZAC_API_URI/informatieobjecten/informatieobjectenList",
                    requestBodyAsString = """
                        {
                            "zaakUUID": "$zaakUuid",
                            "gekoppeldeZaakDocumenten": false
                        }
                    """.trimIndent()
                )
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                // the email PDF should always be the first
                JSONArray(responseBody)[0].toString() shouldEqualJsonIgnoringExtraneousFields """
                {
                  "bestandsnaam" : "subject.pdf",
                  "auteur" : "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                  "beschrijving" : "",
                  "bestandsomvang" : 1851,
                  "creatiedatum" : "$today",
                  "formaat" : "application/pdf",
                  "indicatieGebruiksrecht" : false,
                  "indicaties" : [ "VERZONDEN" ],
                  "informatieobjectTypeOmschrijving" : "e-mail",
                  "informatieobjectTypeUUID" : "$TEST_INFORMATIE_OBJECT_TYPE_1_UUID",
                  "isBesluitDocument" : false,
                  "rechten" : {
                    "lezen" : true,
                    "ondertekenen" : true,
                    "ontgrendelen" : false,
                    "toevoegenNieuweVersie" : false,
                    "vergrendelen" : true,
                    "verwijderen" : false,
                    "wijzigen" : false
                  }
                }
                """.trimIndent()
            }
        }
    }
})
