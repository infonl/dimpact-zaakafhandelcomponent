/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONArray
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

@Suppress("MagicNumber")
class MailtemplateKoppelingRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Context("Mailtemplate koppeling endpoints") {
        Given("Mailtemplate koppelingen have been created as part of the zaaktype CMMN configuration setup") {
            When("the mailtemplate koppeling list is fetched as a beheerder") {
                val response = itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/beheer/mailtemplatekoppeling",
                    testUser = BEHEERDER_1
                )
                lateinit var responseBody: String
                var koppelingId = 0L

                // one koppeling per CMMN zaaktype configured in the itest environment
                val expectedKoppelingenCount = 3

                Then("the response should be 200 HTTP response with 3 koppelingen") {
                    response.code shouldBe HTTP_OK
                    responseBody = response.bodyAsString
                    logger.info { "List response: $responseBody" }
                    val array = JSONArray(responseBody)
                    array.length() shouldBe expectedKoppelingenCount
                    koppelingId = array.getJSONObject(0).getLong("id")
                }

                And("each koppeling uses the zaak-niet-ontvankelijk mailtemplate") {
                    val array = JSONArray(responseBody)
                    for (i in 0 until array.length()) {
                        array.getJSONObject(i).getJSONObject("mailtemplate").also {
                            it.getString("mail") shouldBe MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL
                            it.getString("mailTemplateNaam") shouldBe MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME
                        }
                    }
                }

                And("the first koppeling can be read by its id") {
                    val readResponse = itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplatekoppeling/$koppelingId",
                        testUser = BEHEERDER_1
                    )
                    readResponse.code shouldBe HTTP_OK
                    logger.info { "Read response: ${readResponse.bodyAsString}" }
                    readResponse.bodyAsString shouldEqualJsonIgnoringExtraneousFields """
                        {
                          "id": $koppelingId,
                          "mailtemplate": {
                            "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                            "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME"
                          }
                        }
                    """.trimIndent()
                }
            }

            When("a mailtemplate koppeling is deleted by id") {
                Then("the delete endpoint returns 204 No Content") {
                    val listResponse = itestHttpClient.performGetRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplatekoppeling",
                        testUser = BEHEERDER_1
                    )
                    listResponse.code shouldBe HTTP_OK
                    val existingId = JSONArray(listResponse.bodyAsString).getJSONObject(0).getLong("id")

                    val deleteResponse = itestHttpClient.performDeleteRequest(
                        url = "$ZAC_API_URI/beheer/mailtemplatekoppeling/$existingId",
                        testUser = BEHEERDER_1
                    )
                    deleteResponse.code shouldBe HTTP_NO_CONTENT
                }
            }
        }
    }
})
