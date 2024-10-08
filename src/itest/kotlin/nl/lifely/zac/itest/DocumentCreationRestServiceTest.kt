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
import io.kotest.matchers.string.shouldContain
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.urlEncode
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_SEE_OTHER
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_TITLE
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_ID
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.task1ID
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import okhttp3.FormBody
import okhttp3.Headers
import org.json.JSONObject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
            val endpointUrl = "$ZAC_API_URI/document-creation/create-document-attended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUuid" to zaakProductaanvraag1Uuid,
                        "smartDocumentsTemplateGroupId" to SMART_DOCUMENTS_ROOT_GROUP_ID,
                        "smartDocumentsTemplateId" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID,
                        "title" to SMART_DOCUMENTS_FILE_TITLE
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
    }

    Given("zaak and a file created from template in SmartDocuments") {
        When("SmartDocuments zaak callback is provided with metadata about the new file") {
            val endpointUrl = "$ZAC_API_URI/document-creation/smartdocuments/callback/zaak/$zaakProductaanvraag1Uuid" +
                "?templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID" +
                "&userName=" + TEST_USER_1_NAME.urlEncode() +
                "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode()
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "text/html",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = FormBody.Builder()
                    .add("sdDocument", SMART_DOCUMENTS_FILE_ID)
                    .build(),
                addAuthorizationHeader = true
            )

            Then("The response should contain redirect url to our smart-documents-result page") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_STATUS_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=success"
            }
        }
    }

    Given("zaak, task, title, description, author and a file created from template in SmartDocuments") {
        When("SmartDocuments taak callback is provided with metadata about the new file") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/callback/zaak/$zaakProductaanvraag1Uuid/task/$task1ID" +
                    "?templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID" +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&description=A+file" +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&userName=" + TEST_USER_1_NAME.urlEncode()
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "text/html",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = FormBody.Builder()
                    .add("sdDocument", SMART_DOCUMENTS_FILE_ID)
                    .build(),
                addAuthorizationHeader = true
            )

            Then("The response should contain redirect url, doc name, zaak and taak ids") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_STATUS_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION" +
                    "&taak=$task1ID" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=success"
            }
        }
    }

    Given("zaak and a file creation cancelled in SmartDocuments") {
        When("SmartDocuments zaak callback is called") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/callback/zaak/$zaakProductaanvraag1Uuid" +
                    "?templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID" +
                    "&userName=" + TEST_USER_1_NAME.urlEncode() +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode()
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "text/html",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = FormBody.Builder().build(),
                addAuthorizationHeader = true
            )

            Then("The response should contain redirect url, zaak id") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_STATUS_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=cancelled"
            }
        }
    }

    Given("zaak, task and a file creation cancelled in SmartDocuments") {
        When("SmartDocuments taak callback is called") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/callback/zaak/$zaakProductaanvraag1Uuid/task/$task1ID" +
                    "?templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID" +
                    "&userName=" + TEST_USER_1_NAME.urlEncode() +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode()
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "text/html",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = FormBody.Builder().build(),
                addAuthorizationHeader = true
            )

            Then("The response should contain redirect url, zaak and taak ids") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_STATUS_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_PRODUCTAANVRAAG_1_IDENTIFICATION" +
                    "&taak=$task1ID" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=cancelled"
            }
        }
    }
})
