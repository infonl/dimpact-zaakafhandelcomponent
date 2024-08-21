/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_EXTENSION
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.task1ID
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import org.json.JSONObject

/**
 * This test assumes that a zaak has been created, a task has been started and a template mapping is created
 * in previously run tests.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
class DocumentCreationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("ZAC and all related Docker containers are running and zaak exists") {
        When("the create document attended ('wizard') endpoint is called with a zaak UUID") {
            val endpointUrl = "$ZAC_API_URI/documentcreation/createdocumentattended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUUID" to zaakProductaanvraag1Uuid
                    )
                ).toString()
            )
            Then("the response should be OK and the response should contain a redirect URL to Smartdocuments") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK

                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "redirectURL",
                        "$SMART_DOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=dummySmartdocumentsTicketID"
                    )
                }
            }
        }
        When("the create document unattended endpoint is called with a zaak UUID") {
            val endpointUrl = "$ZAC_API_URI/documentcreation/createdocumentunattended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "smartDocumentsTemplateGroupName" to SMART_DOCUMENTS_ROOT_GROUP_NAME,
                        "smartDocumentsTemplateName" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME,
                        "zaakUuid" to zaakProductaanvraag1Uuid
                    )
                ).toString()
            )
            Then("the response should be OK") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "message",
                        "SmartDocuments document with filename: '$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME" +
                            ".$SMART_DOCUMENTS_FILE_EXTENSION' was created and stored successfully in the zaakregister."
                    )
                }
            }
        }
        When("the create document unattended endpoint is called with a zaak UUID and a task ID") {
            val endpointUrl = "$ZAC_API_URI/documentcreation/createdocumentunattended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "smartDocumentsTemplateGroupName" to SMART_DOCUMENTS_ROOT_GROUP_NAME,
                        "smartDocumentsTemplateName" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME,
                        "taskId" to task1ID,
                        "zaakUuid" to zaakProductaanvraag1Uuid
                    )
                ).toString()
            )
            Then("the response should be OK") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "message",
                        "SmartDocuments document with filename: '$SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME" +
                            ".$SMART_DOCUMENTS_FILE_EXTENSION' was created and stored successfully in the zaakregister."
                    )
                }
            }
        }
    }
})
