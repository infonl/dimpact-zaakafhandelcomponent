/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.client.urlEncode
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.OLD_IAM_TEST_USER_1
import nl.info.zac.itest.config.SMART_DOCUMENTS_FILE_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_FILE_TITLE
import nl.info.zac.itest.config.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_GROUP_NAME
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_TEMPLATE_1_NAME
import okhttp3.FormBody
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_SEE_OTHER
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class BpmnDocumentCreationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    lateinit var bpmnZaakUuid: UUID
    lateinit var taskId: String

    beforeSpec {
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)

        bpmnZaakUuid = zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_1_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        taskId = itestHttpClient.performGetRequest("$ZAC_API_URI/taken/zaak/$bpmnZaakUuid").let {
            val responseBody = it.bodyAsString
            logger.info { "Response: $responseBody" }
            it.code shouldBe HTTP_OK
            responseBody.shouldBeJsonArray()
            JSONArray(responseBody).length() shouldBe 1
            JSONArray(responseBody).getJSONObject(0).getString("id")
        }
    }

    Given("BPMN test process is available in ZAC, a BPMN zaak and task exists and a behandelaar is logged in") {
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
                        "author" to FAKE_AUTHOR_NAME
                    )
                ).toString()
            )
            Then("the response should be OK and the response should contain a redirect URL to SmartDocuments") {
                val responseBody = response.bodyAsString
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
                        "author" to FAKE_AUTHOR_NAME,
                        "creationDate" to ZonedDateTime.now()
                    )
                ).toString()
            )
            Then("the response should be OK and the response should contain a redirect URL to SmartDocuments") {
                val responseBody = response.bodyAsString
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
                "?userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
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
                    .build()
            )

            Then("The response should contain redirect url to our smart-documents-result page") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                // the zaak identification is generated dynamically on every test run
                // so we do not check for its value here
                locationHeader shouldContain "static/smart-documents-result.html?zaak="
                locationHeader shouldContain "&taak=$taskId" +
                    "&doc=${SMART_DOCUMENTS_FILE_TITLE.urlEncode()}" +
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
                    "&userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
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
                    .build()
            )

            Then("The response should contain redirect url, doc name, zaak and taak ids") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                // the zaak identification is generated dynamically on every test run
                // so we do not check for its value here
                locationHeader shouldContain "static/smart-documents-result.html?zaak="
                locationHeader shouldContain "&taak=$taskId" +
                    "&doc=${SMART_DOCUMENTS_FILE_TITLE.urlEncode()}" +
                    "&result=success"
            }
        }
    }

    Given("zaak, task and a file creation cancelled in SmartDocuments") {
        When("SmartDocuments taak callback is called") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/bpmn-callback/" +
                    "zaak/$bpmnZaakUuid/task/$taskId" +
                    "?userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
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
                requestBody = FormBody.Builder().build()
            )

            Then("The response should contain redirect url, zaak and taak ids") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                // the zaak identification is generated dynamically on every test run
                // so we do not check for its value here
                locationHeader shouldContain "static/smart-documents-result.html?zaak="
                locationHeader shouldContain "&taak=$taskId" +
                    "&doc=${SMART_DOCUMENTS_FILE_TITLE.urlEncode()}" +
                    "&result=cancelled"
            }
        }
    }
})
