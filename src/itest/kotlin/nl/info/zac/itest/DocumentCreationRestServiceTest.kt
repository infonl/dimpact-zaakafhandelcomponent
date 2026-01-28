/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.urlEncode
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.OLD_IAM_TEST_USER_1
import nl.info.zac.itest.config.SMART_DOCUMENTS_FILE_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_FILE_TITLE
import nl.info.zac.itest.config.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_GROUP_ID
import nl.info.zac.itest.config.SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID
import okhttp3.FormBody
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_SEE_OTHER
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class DocumentCreationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val taskHelper = TaskHelper(zacClient)
    lateinit var taskId: String
    lateinit var zaakUuid: String
    lateinit var zaakIdentification: String

    Given(
        """
        A zaak exists, a task has been started, a SmartDocuments template mapping has been created,
        and a behandelaar is logged in
        """
    ) {
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_3_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHEERDER_ELK_ZAAKTYPE
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid")
                zaakIdentification = getString("identificatie")
            }
        }
        taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid.let(UUID::fromString),
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = BEHANDELAARS_DOMAIN_TEST_1,
            testUser = BEHEERDER_ELK_ZAAKTYPE
        )

        When("the create document attended ('wizard') endpoint is called on the zaak") {
            val endpointUrl = "$ZAC_API_URI/document-creation/create-document-attended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUuid" to zaakUuid,
                        "smartDocumentsTemplateGroupId" to SMART_DOCUMENTS_ROOT_GROUP_ID,
                        "smartDocumentsTemplateId" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID,
                        "title" to SMART_DOCUMENTS_FILE_TITLE,
                        "author" to FAKE_AUTHOR_NAME,
                        "creationDate" to ZonedDateTime.now()
                    )
                ).toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
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

        When("the create document attended ('wizard') endpoint is called on the task") {
            val endpointUrl = "$ZAC_API_URI/document-creation/create-document-attended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUuid" to zaakUuid,
                        "taskUuid" to taskId,
                        "smartDocumentsTemplateGroupId" to SMART_DOCUMENTS_ROOT_GROUP_ID,
                        "smartDocumentsTemplateId" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID,
                        "title" to SMART_DOCUMENTS_FILE_TITLE,
                        "description" to "document description",
                        "author" to FAKE_AUTHOR_NAME,
                        "creationDate" to ZonedDateTime.now()
                    )
                ).toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
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

        When("the create document attended ('wizard') endpoint is called without mandatory parameter") {
            val endpointUrl = "$ZAC_API_URI/document-creation/create-document-attended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUuid" to zaakUuid,
                        "smartDocumentsTemplateGroupId" to SMART_DOCUMENTS_ROOT_GROUP_ID,
                        "smartDocumentsTemplateId" to SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID,
                    )
                ).toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            Then("the response should be 400 Client Error") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_BAD_REQUEST
                responseBody shouldContain "must not be null"
            }
        }
    }

    Given("zaak and a file created from template in SmartDocuments") {
        When("SmartDocuments zaak callback is provided with metadata about the new file") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/cmmn-callback/zaak/$zaakUuid" +
                    "?userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID"

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
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("The response should contain redirect url to our smart-documents-result page") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$zaakIdentification" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=success"
            }
        }
    }

    Given("zaak, task, title, description, author and a file created from template in SmartDocuments") {
        When("SmartDocuments taak callback is provided with metadata about the new file") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/cmmn-callback/" +
                    "zaak/$zaakUuid/task/$taskId" +
                    "?title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&description=A+file" +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
                    "&templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID"

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
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("The response should contain redirect url, doc name, zaak and taak ids") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$zaakIdentification" +
                    "&taak=$taskId" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=success"
            }
        }
    }

    Given("zaak and a file creation cancelled in SmartDocuments") {
        When("SmartDocuments zaak callback is called") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/cmmn-callback/zaak/$zaakUuid" +
                    "?userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID"

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
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("The response should contain redirect url, zaak id") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$zaakIdentification" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=cancelled"
            }
        }
    }

    Given("zaak, task and a file creation cancelled in SmartDocuments") {
        When("SmartDocuments taak callback is called") {
            val endpointUrl =
                "$ZAC_API_URI/document-creation/smartdocuments/cmmn-callback/" +
                    "zaak/$zaakUuid/task/$taskId" +
                    "?userName=" + OLD_IAM_TEST_USER_1.displayName.urlEncode() +
                    "&title=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&creationDate=" + ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).urlEncode() +
                    "&templateGroupId=$SMART_DOCUMENTS_ROOT_GROUP_ID" +
                    "&templateId=$SMART_DOCUMENTS_ROOT_TEMPLATE_1_ID"

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
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("The response should contain redirect url, zaak and taak ids") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                val locationHeader = response.headers["Location"]
                logger.info { "Location header: $locationHeader" }

                response.code shouldBe HTTP_SEE_OTHER
                locationHeader shouldContain "static/smart-documents-result.html" +
                    "?zaak=$zaakIdentification" +
                    "&taak=$taskId" +
                    "&doc=" + SMART_DOCUMENTS_FILE_TITLE.urlEncode() +
                    "&result=cancelled"
            }
        }
    }
})
