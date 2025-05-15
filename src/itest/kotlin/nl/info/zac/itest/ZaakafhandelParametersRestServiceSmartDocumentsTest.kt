/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import java.net.HttpURLConnection.HTTP_BAD_REQUEST

@Order(TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED)
class ZaakafhandelParametersRestServiceSmartDocumentsTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list SmartDocuments templates endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/smartdocuments-templates"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrder """
                [
                  {
                    "groups": [
                      {
                        "groups": [],
                        "id": "$SMART_DOCUMENTS_GROUP_1_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_1_NAME",
                        "templates": [
                          {
                            "id": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID",
                            "name": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME"
                          },
                          {
                            "id": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID",
                            "name": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME"
                          }
                        ]
                      },
                      {
                        "groups": [],
                        "id": "$SMART_DOCUMENTS_GROUP_2_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_2_NAME",
                        "templates": [
                          {
                            "id": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID",
                            "name": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME"
                          },
                          {
                            "id": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID",
                            "name": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME"
                          }
                        ]
                      }
                    ],
                    "id": "$SMART_DOCUMENTS_ROOT_GROUP_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_GROUP_NAME",
                    "templates": [
                      {
                        "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID",
                        "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME"
                      },
                      {
                        "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID",
                        "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME"
                      }
                    ]
                  }
                ]
                """.trimIndent()
            }
        }

        When("the list SmartDocuments template names endpoint is called") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/smartdocuments-group-template-names",
                requestBodyAsString = """
                {
                    "groups": [ "$SMART_DOCUMENTS_ROOT_GROUP_NAME", "$SMART_DOCUMENTS_GROUP_1_NAME" ]
                }
                """.trimIndent()
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody shouldEqualJsonIgnoringOrder """
                [ "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME", "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME" ]                    
                """.trimIndent()
            }
        }

        When("the create mapping endpoint is called with correct payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelparameters/" +
                "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID/smartdocuments-templates-mapping"
            val restTemplateGroups = """
            [
              {
                "id": "$SMART_DOCUMENTS_ROOT_GROUP_ID",
                "name": "$SMART_DOCUMENTS_ROOT_GROUP_NAME",
                "groups": [
                  {
                    "groups": [],
                    "id": "$SMART_DOCUMENTS_GROUP_1_ID",
                    "name": "$SMART_DOCUMENTS_GROUP_1_NAME",
                    "templates": [
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      },
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      }
                    ]
                  },
                  {
                    "groups": [],
                    "id": "$SMART_DOCUMENTS_GROUP_2_ID",
                    "name": "$SMART_DOCUMENTS_GROUP_2_NAME",
                    "templates": [
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      },
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID",
                        "name": "$SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      }
                    ]
                  }
                ],
                "templates": [
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME",
                    "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                  },
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME",
                    "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                  }
                ]
              }
            ]
            """.trimIndent()
            val storeResponse = itestHttpClient.performJSONPostRequest(
                url = smartDocumentsZaakafhandelParametersUrl,
                requestBodyAsString = restTemplateGroups
            )
            val storeBody = storeResponse.body!!.string()
            logger.info { "Response: $storeBody" }
            storeResponse.isSuccessful shouldBe true

            And("then the mapping is fetched back") {
                val fetchResponse = itestHttpClient.performGetRequest(url = smartDocumentsZaakafhandelParametersUrl)

                Then("the data is fetched correctly") {
                    val fetchResponseBody = fetchResponse.body!!.string()
                    logger.info { "Response: $fetchResponseBody" }

                    fetchResponse.isSuccessful shouldBe true
                    fetchResponseBody shouldEqualJsonIgnoringOrder restTemplateGroups
                }
            }
        }

        When("the create mapping endpoint is called with invalid payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelparameters/" +
                "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID/smartdocuments-templates-mapping"
            val restTemplateGroups = """
            [
              {
                "id": "$SMART_DOCUMENTS_ROOT_GROUP_ID",
                "name": "$SMART_DOCUMENTS_ROOT_GROUP_NAME",
                "groups": [
                  {
                    "groups": [],
                    "id": "$SMART_DOCUMENTS_GROUP_1_ID",
                    "name": "group A",
                    "templates": [
                      {
                        "id": "4",
                        "name": "group A template 1",
                        "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                      }
                    ]
                  }
                ],
                "templates": [
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME",
                    "informatieObjectTypeUUID": "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"
                  }
                ]
              }
            ]
            """.trimIndent()
            val storeResponse = itestHttpClient.performJSONPostRequest(
                url = smartDocumentsZaakafhandelParametersUrl,
                requestBodyAsString = restTemplateGroups
            )

            Then("the request errors") {
                val storeResponseBody = storeResponse.body!!.string()
                logger.info { "Response: $storeResponseBody" }

                storeResponse.code shouldBe HTTP_BAD_REQUEST
                storeResponseBody shouldBe """{"message":"msg.error.smartdocuments.not.configured"}"""
            }
        }
    }
})
