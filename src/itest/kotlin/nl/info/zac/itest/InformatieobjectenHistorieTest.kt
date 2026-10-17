/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.schema.array
import io.kotest.assertions.json.schema.jsonSchema
import io.kotest.assertions.json.schema.obj
import io.kotest.assertions.json.schema.shouldMatchSchema
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.match
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_DEFINITIEF
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_UPDATED_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields

@OptIn(ExperimentalKotest::class)
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
class InformatieobjectenHistorieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaak exists for which there is an uploaded document") {
        When("informatieobjecten historie is requested") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUUID/historie"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                val expectedResponse = """[
                  {
                    "actie": "GEWIJZIGD",
                    "attribuutLabel": "registratiedatum",
                    "door": "$TEST_USER_1_NAME",
                    "toelichting": "Door ondertekenen"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "versie",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "3",
                    "oudeWaarde": "2",
                    "toelichting": "Door ondertekenen"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "informatieobject.status",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$DOCUMENT_STATUS_DEFINITIEF",
                    "oudeWaarde": "$DOCUMENT_STATUS_IN_BEWERKING",
                    "toelichting": "Door ondertekenen"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "ondertekening",
                    "door": "$TEST_USER_1_NAME",
                    "toelichting": "Door ondertekenen"
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "titel",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$DOCUMENT_UPDATED_FILE_TITLE",
                    "oudeWaarde": "$DOCUMENT_FILE_TITLE",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "bestandsnaam",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$TEST_TXT_FILE_NAME",
                    "oudeWaarde": "$TEST_PDF_FILE_NAME",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "documentType",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$INFORMATIE_OBJECT_TYPE_FACTUUR_OMSCHRIJVING",
                    "oudeWaarde": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "registratiedatum",
                    "door": "$TEST_USER_1_NAME",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "versie",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "2",
                    "oudeWaarde": "1",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEWIJZIGD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "formaat",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$TEXT_MIME_TYPE",
                    "oudeWaarde": "$PDF_MIME_TYPE",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "indicatieGebruiksrecht",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "geen",
                    "toelichting": ""
                  },
                  {
                    "actie": "GEKOPPELD",
                    "applicatie": "ZAC",
                    "attribuutLabel": "informatieobject",
                    "door": "$TEST_USER_1_NAME",
                    "nieuweWaarde": "$DOCUMENT_1_IDENTIFICATION",
                    "toelichting": ""
                  }
                ]
                """.trimIndent()

                responseBody shouldMatchSchema jsonSchema {
                    array {
                        obj {
                            string("actie")
                            string("applicatie")
                            string("attribuutLabel")
                            string("datumTijd") {
                                // "2024-07-16T10:46:53.405553Z"
                                match("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{6}Z""".toRegex())
                            }
                            string("door")
                            string("nieuweWaarde")
                            string("oudeWaarde", optional = true)
                            string("toelichting")
                        }
                    }
                }
                responseBody shouldEqualJsonIgnoringExtraneousFields expectedResponse
            }
        }
    }
})
