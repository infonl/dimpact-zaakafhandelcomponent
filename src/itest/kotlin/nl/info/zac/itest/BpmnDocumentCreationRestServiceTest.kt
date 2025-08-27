/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.urlEncode
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_ID
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_GROUP_NAME
import nl.info.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_BEHANDELAARS_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_BEHANDELAARS_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_BPMN_TEST_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.FormBody
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_SEE_OTHER
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * This test assumes that a zaak has been created, a task has been started, and a template mapping is created
 * in previously run tests.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
class BpmnDocumentCreationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    var bpmnZaakUuid = zacClient.createZaak(
        zaakTypeUUID = ZAAKTYPE_BPMN_TEST_UUID,
        groupId = TEST_GROUP_BEHANDELAARS_ID,
        groupName = TEST_GROUP_BEHANDELAARS_DESCRIPTION,
        startDate = DATE_TIME_2000_01_01
    ).run {
        val responseBody = body.string()
        logger.info { "Response: $responseBody" }
        code shouldBe HTTP_OK
        JSONObject(responseBody).run {
            getJSONObject("zaakdata").run {
                getString("zaakUUID").run(UUID::fromString)
            }
        }
    }
    var taskId = itestHttpClient.performGetRequest("$ZAC_API_URI/taken/zaak/$bpmnZaakUuid").let {
        val responseBody = it.body.string()
        logger.info { "Response: $responseBody" }
        it.isSuccessful shouldBe true
        responseBody.shouldBeJsonArray()
        JSONArray(responseBody).length() shouldBe 1
        JSONArray(responseBody).getJSONObject(0).getString("id")
    }

    Given("BPMN zaak $bpmnZaakUuid with task $taskId exists") {
        When("the create document attended ('wizard') endpoint is called with minimum set of parameters") {
            val endpointUrl = "$ZAC_API_URI/document-creation/create-document-attended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUuid" to bpmnZaakUuid,
                        "taskId" to taskId,
                        "informatieobjecttypeUuid" to INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID,
                        "smartDocumentsTemplateGroupName" to SMART_DOCUMENTS_ROOT_GROUP_NAME,
                        "smartDocumentsTemplateName" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME,
                        "title" to SMART_DOCUMENTS_FILE_TITLE,
                        "creationDate" to ZonedDateTime.now(),
                        "author" to TEST_USER_1_NAME
                    )
                ).toString()
            )
            Then("the response should be OK and the response should contain a redirect URL to SmartDocuments") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK

                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "redirectURL",
                        "$SMART_DOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=fakeSmartDocumentsTicketID"
                    )
                }
            }
        }

        When("the create document attended ('wizard') endpoint is called with all parameters") {
            val endpointUrl = "$ZAC_API_URI/document-creation/create-document-attended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUuid" to bpmnZaakUuid,
                        "taskId" to taskId,
                        "informatieobjecttypeUuid" to INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID,
                        "smartDocumentsTemplateGroupName" to SMART_DOCUMENTS_ROOT_GROUP_NAME,
                        "smartDocumentsTemplateName" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME,
                        "title" to SMART_DOCUMENTS_FILE_TITLE,
                        "description" to "document description",
                        "author" to TEST_USER_1_NAME,
                        "creationDate" to ZonedDateTime.now()
                    )
                ).toString()
            )
            Then("the response should be OK and the response should contain a redirect URL to SmartDocuments") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK

                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "redirectURL",
                        "$SMART_DOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=fakeSmartDocumentsTicketID"
                    )
                }
            }
        }
    }

    Given("zaak and a file created from template in SmartDocuments") {
        When("SmartDocuments zaak callback is provided with metadata about the new file") {
            val endpointUrl = "$ZAC_API_URI/document-creation/smartdocuments/bpmn-callback/" +
                "zaak/$bpmnZaakUuid/task/$taskId" +
                "?userName=" + TEST_USER_1_NAME.urlEncode() +
                "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                "&templateGroupName=$SMART_DOCUMENTS_ROOT_GROUP_NAME" +
                "&templateName=" + SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME.urlEncode() +
                "&informatieobjecttypeUuid=$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"

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
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_BPMN_TEST_IDENTIFICATION&taak=$taskId" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=success"
            }
        }
    }

    Given("zaak, task, title, description, author and a file created from template in SmartDocuments") {
        When("SmartDocuments taak callback is provided with metadata about the new file") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/bpmn-callback/" +
                    "zaak/$bpmnZaakUuid/task/$taskId" +
                    "?title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&description=A+file" +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&userName=" + TEST_USER_1_NAME.urlEncode() +
                    "&templateGrouName=$SMART_DOCUMENTS_ROOT_GROUP_NAME" +
                    "&templateName=" + SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME.urlEncode() +
                    "&informatieobjecttypeUuid=$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"

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
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_BPMN_TEST_IDENTIFICATION" +
                    "&taak=$taskId" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=success"
            }
        }
    }

    Given("zaak, task and a file creation cancelled in SmartDocuments") {
        When("SmartDocuments taak callback is called") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/bpmn-callback/" +
                    "zaak/$bpmnZaakUuid/task/$taskId" +
                    "?userName=" + TEST_USER_1_NAME.urlEncode() +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&templateGrouName=$SMART_DOCUMENTS_ROOT_GROUP_NAME" +
                    "&templateName=" + SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME.urlEncode() +
                    "&informatieobjecttypeUuid=$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID"

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
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                val locationHeader = response.header("Location")!!
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$ZAAK_BPMN_TEST_IDENTIFICATION" +
                    "&taak=$taskId" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=cancelled"
            }
        }
    }
})
