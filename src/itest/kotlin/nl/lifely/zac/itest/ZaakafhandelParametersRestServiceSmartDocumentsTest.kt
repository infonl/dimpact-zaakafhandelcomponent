/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_BAD_REQUEST
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.util.shouldEqualJsonIgnoringOrder

@Order(TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED)
class ZaakafhandelParametersRestServiceSmartDocumentsTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list SmartDocuments templates endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters/document-templates"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("$[0].id", SMART_DOCUMENTS_ROOT_GROUP_ID)
                    shouldContainJsonKeyValue("$[0].name", SMART_DOCUMENTS_ROOT_GROUP_NAME)

                    shouldContainJsonKeyValue("$[0].templates[0].id", SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID)
                    shouldContainJsonKeyValue("$[0].templates[0].name", SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME)
                    shouldContainJsonKeyValue("$[0].templates[1].id", SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID)
                    shouldContainJsonKeyValue("$[0].templates[1].name", SMART_DOCUMENTS_ROOT_TEMPLATE_2_NAME)

                    shouldContainJsonKeyValue("$[0].groups[0].id", SMART_DOCUMENTS_GROUP_1_ID)
                    shouldContainJsonKeyValue("$[0].groups[0].name", SMART_DOCUMENTS_GROUP_1_NAME)
                    shouldContainJsonKeyValue("$[0].groups[0].templates[0].id", SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID)
                    shouldContainJsonKeyValue(
                        "$[0].groups[0].templates[0].name",
                        SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_NAME
                    )
                    shouldContainJsonKeyValue("$[0].groups[0].templates[1].id", SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID)
                    shouldContainJsonKeyValue(
                        "$[0].groups[0].templates[1].name",
                        SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_NAME
                    )

                    shouldContainJsonKeyValue("$[0].groups[1].id", SMART_DOCUMENTS_GROUP_2_ID)
                    shouldContainJsonKeyValue("$[0].groups[1].name", SMART_DOCUMENTS_GROUP_2_NAME)
                    shouldContainJsonKeyValue("$[0].groups[1].templates[0].id", SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID)
                    shouldContainJsonKeyValue(
                        "$[0].groups[1].templates[0].name",
                        SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_NAME
                    )
                    shouldContainJsonKeyValue("$[0].groups[1].templates[1].id", SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID)
                    shouldContainJsonKeyValue(
                        "$[0].groups[1].templates[1].name",
                        SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_NAME
                    )
                }
            }
        }

        When("the create mapping endpoint is called with correct payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelparameters/" +
                "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID/document-templates"
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
                "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID/document-templates"
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

                storeResponse.code shouldBe HTTP_STATUS_BAD_REQUEST
                storeResponseBody shouldContain
                    "group.$SMART_DOCUMENTS_ROOT_GROUP_ID.$SMART_DOCUMENTS_ROOT_GROUP_NAME." +
                    "group.$SMART_DOCUMENTS_GROUP_1_ID.group A"
                storeResponseBody shouldContain
                    "group.$SMART_DOCUMENTS_ROOT_GROUP_ID.$SMART_DOCUMENTS_ROOT_GROUP_NAME." +
                    "group.$SMART_DOCUMENTS_GROUP_1_ID.group A.template.4.group A template 1"
            }
        }
    }
})
