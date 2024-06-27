/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.compareJsonOptions
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
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
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

class SmartDocumentsTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    infix fun String.lenientShouldEqualJson(other: String) = this.shouldEqualJson(
        other,
        compareJsonOptions {
            arrayOrder = ArrayOrder.Lenient
        }
    )

    Given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list SmartDocuments templates endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaakafhandelParameters/documentTemplates"
            )

            Then("the response should be ok") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("$[0].id", SMART_DOCUMENTS_ROOT_GROUP_ID)
                    shouldContainJsonKeyValue("$[0].name", "root")

                    shouldContainJsonKeyValue("$[0].templates[0].id", SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID)
                    shouldContainJsonKeyValue("$[0].templates[0].name", "root template 1")
                    shouldContainJsonKeyValue("$[0].templates[1].id", SMART_DOCUMENTS_ROOT_TEMPLATE_2_ID)
                    shouldContainJsonKeyValue("$[0].templates[1].name", "root template 2")

                    shouldContainJsonKeyValue("$[0].groups[0].id", SMART_DOCUMENTS_GROUP_1_ID)
                    shouldContainJsonKeyValue("$[0].groups[0].name", "group 1")
                    shouldContainJsonKeyValue("$[0].groups[0].templates[0].id", SMART_DOCUMENTS_GROUP_1_TEMPLATE_1_ID)
                    shouldContainJsonKeyValue("$[0].groups[0].templates[0].name", "group 1 template 1")
                    shouldContainJsonKeyValue("$[0].groups[0].templates[1].id", SMART_DOCUMENTS_GROUP_1_TEMPLATE_2_ID)
                    shouldContainJsonKeyValue("$[0].groups[0].templates[1].name", "group 1 template 2")

                    shouldContainJsonKeyValue("$[0].groups[1].id", SMART_DOCUMENTS_GROUP_2_ID)
                    shouldContainJsonKeyValue("$[0].groups[1].name", "group 2")
                    shouldContainJsonKeyValue("$[0].groups[1].templates[0].id", SMART_DOCUMENTS_GROUP_2_TEMPLATE_1_ID)
                    shouldContainJsonKeyValue("$[0].groups[1].templates[0].name", "group 2 template 1")
                    shouldContainJsonKeyValue("$[0].groups[1].templates[1].id", SMART_DOCUMENTS_GROUP_2_TEMPLATE_2_ID)
                    shouldContainJsonKeyValue("$[0].groups[1].templates[1].name", "group 2 template 2")
                }
            }
        }

        When("the create mapping endpoint is called with correct payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelParameters/" +
                "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID/informatieobjectType/" +
                "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID/documentTemplates"
            val restTemplateGroups = """
                [
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_GROUP_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_GROUP_NAME",
                    "groups": [
                      {
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
                    fetchResponseBody lenientShouldEqualJson restTemplateGroups
                }
            }
        }

        When("the create mapping endpoint is called with invalid payload") {
            val smartDocumentsZaakafhandelParametersUrl = "$ZAC_API_URI/zaakafhandelParameters/" +
                "$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID/informatieobjectType/" +
                "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID/documentTemplates"
            val restTemplateGroups = """
                [
                  {
                    "id": "$SMART_DOCUMENTS_ROOT_GROUP_ID",
                    "name": "$SMART_DOCUMENTS_ROOT_GROUP_NAME",
                    "groups": [
                      {
                        "id": "$SMART_DOCUMENTS_GROUP_1_ID",
                        "name": "group A",
                        "templates": [
                          {
                            "id": "4",
                            "name": "group A template 1"
                          }
                        ]
                      }
                    ],
                    "templates": [
                      {
                        "id": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID",
                        "name": "$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME"
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
