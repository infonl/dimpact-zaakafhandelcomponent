/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_1_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_1_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_2_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_2_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_GROUP_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_GROUP_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_TEMPLATE_MAPPINGS
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrder
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

class ZaaktypeCmmnConfigurationRestServiceSmartDocumentsTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        """
        ZAC Docker container is running and zaaktypeCmmnConfiguration have been created,
        and a beheerder is logged in
        """
    ) {
        When("the list SmartDocuments templates endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/smartdocuments-templates",
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )

            Then("the response should be ok") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
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
                        "path": [ "$SMART_DOCUMENTS_ROOT_GROUP_NAME", "$SMART_DOCUMENTS_GROUP_1_NAME" ]
                    }
                """.trimIndent(),
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )

            Then("the response should be ok") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldEqualJsonIgnoringOrder """
                [ "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME", "$SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME" ]                    
                """.trimIndent()
            }
        }

        When("the create mapping endpoint is called with correct payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelparameters/" +
                "$ZAAKTYPE_TEST_3_UUID/smartdocuments-templates-mapping"
            val storeResponse = itestHttpClient.performJSONPostRequest(
                url = smartDocumentsZaakafhandelParametersUrl,
                requestBodyAsString = SMART_DOCUMENTS_TEMPLATE_MAPPINGS,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )
            val storeBody = storeResponse.bodyAsString
            logger.info { "Response: $storeBody" }
            storeResponse.code shouldBe HTTP_NO_CONTENT

            And("then the mapping is fetched back") {
                val fetchResponse = itestHttpClient.performGetRequest(
                    url = smartDocumentsZaakafhandelParametersUrl,
                    testUser = BEHEERDER_ELK_ZAAKTYPE
                )

                Then("the data is fetched correctly") {
                    val fetchResponseBody = fetchResponse.bodyAsString
                    logger.info { "Response: $fetchResponseBody" }

                    fetchResponse.code shouldBe HTTP_OK
                    fetchResponseBody shouldEqualJsonIgnoringOrder SMART_DOCUMENTS_TEMPLATE_MAPPINGS
                }
            }
        }

        When("the create mapping endpoint is called with invalid payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelparameters/" +
                "$ZAAKTYPE_TEST_3_UUID/smartdocuments-templates-mapping"
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
                requestBodyAsString = restTemplateGroups,
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )

            Then("the request errors") {
                val storeResponseBody = storeResponse.bodyAsString
                logger.info { "Response: $storeResponseBody" }

                storeResponse.code shouldBe HTTP_BAD_REQUEST
                storeResponseBody shouldBe """{"message":"msg.error.smartdocuments.not.configured"}"""
            }
        }
    }
})
