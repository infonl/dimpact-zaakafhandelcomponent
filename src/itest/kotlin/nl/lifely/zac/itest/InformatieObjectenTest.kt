/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_SIZE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_SIZE
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.lifely.zac.itest.config.ItestConfiguration.task1ID
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * This test assumes a zaak has been created, and a task has been started in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED)
class InformatieObjectenTest : BehaviorSpec({
    val fileTitle = "dummyTitel"
    val updatedFileTitle = "updated title with Špëcîål characters"
    val documentVertrouwelijkheidsAanduidingVertrouwelijk = "zaakvertrouwelijk"
    val documentVertrouwelijkheidsAanduidingOpenbaar = "openbaar"
    val documentStatusDefinitief = "definitief"
    val documentStatusInBewerking = "in_bewerking"

    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        "ZAC and all related Docker containers are running and zaak exists"
    ) {
        When("the create document informatie objecten endpoint is called") {
            val endpointUrl = "$ZAC_API_URI/informatieobjecten/documentcreatie"
            logger.info { "Calling $endpointUrl endpoint" }

            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUUID" to zaakProductaanvraag1Uuid
                    )
                ).toString()
            )
            Then(
                "the response should be OK and the response should contain a redirect URL to Smartdocuments"
            ) {
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
    Given(
        "ZAC and all related Docker containers are running and zaak exists"
    ) {
        When("the create enkelvoudig informatie object with file upload endpoint is called for the zaak") {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakProductaanvraag1Uuid/$zaakProductaanvraag1Uuid"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_PDF_FILE_NAME).let {
                File(it!!.path)
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_PDF_FILE_NAME)
                    .addFormDataPart("titel", fileTitle)
                    .addFormDataPart("bestandsomvang", file.length().toString())
                    .addFormDataPart("formaat", PDF_MIME_TYPE)
                    .addFormDataPart(
                        "file",
                        TEST_PDF_FILE_NAME,
                        file.asRequestBody(PDF_MIME_TYPE.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        documentVertrouwelijkheidsAanduidingVertrouwelijk
                    )
                    .addFormDataPart("status", documentStatusInBewerking)
                    .addFormDataPart(
                        "creatiedatum",
                        DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd'T'HH:mm+01:00"
                        ).format(ZonedDateTime.now())
                    )
                    .addFormDataPart("auteur", TEST_USER_1_NAME)
                    .addFormDataPart("taal", "dut")
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody
            )

            Then(
                "the response should be OK and contain information for the created document and uploaded file"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("auteur", TEST_USER_1_NAME)
                    shouldContainJsonKeyValue("status", documentStatusInBewerking)
                    shouldContainJsonKeyValue("taal", "Nederlands")
                    shouldContainJsonKeyValue("titel", fileTitle)
                    shouldContainJsonKeyValue(
                        "vertrouwelijkheidaanduiding",
                        documentVertrouwelijkheidsAanduidingVertrouwelijk
                    )
                    shouldContainJsonKeyValue(
                        "informatieobjectTypeOmschrijving",
                        INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
                    )
                    shouldContainJsonKey("informatieobjectTypeUUID")
                    shouldContainJsonKey("identificatie")
                    shouldContainJsonKeyValue("bestandsnaam", TEST_PDF_FILE_NAME)
                    shouldContainJsonKeyValue("bestandsomvang", file.length().toString())
                    shouldContainJsonKeyValue("formaat", PDF_MIME_TYPE)
                }
                enkelvoudigInformatieObjectUUID = JSONObject(responseBody).getString("uuid")
            }
        }
        When("update of enkelvoudig informatie object with file upload endpoint is called") {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/update"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_TXT_FILE_NAME).let {
                File(it!!.path)
            }

            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("uuid", enkelvoudigInformatieObjectUUID)
                    .addFormDataPart("zaakUuid", zaakProductaanvraag1Uuid.toString())
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart("bestandsnaam", TEST_TXT_FILE_NAME)
                    .addFormDataPart("titel", updatedFileTitle)
                    .addFormDataPart("bestandsomvang", TEST_TXT_FILE_SIZE.toString())
                    .addFormDataPart("formaat", TEXT_MIME_TYPE)
                    .addFormDataPart(
                        "file",
                        TEST_TXT_FILE_NAME,
                        file.asRequestBody(TEXT_MIME_TYPE.toMediaType())
                    )
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody
            )
            Then(
                "the response should be OK and should contain information about the updates"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("auteur", TEST_USER_1_NAME)
                    shouldContainJsonKeyValue("status", documentStatusInBewerking)
                    shouldContainJsonKeyValue("taal", "Nederlands")
                    shouldContainJsonKeyValue("titel", updatedFileTitle)
                    shouldContainJsonKeyValue(
                        "vertrouwelijkheidaanduiding",
                        documentVertrouwelijkheidsAanduidingVertrouwelijk
                    )
                    shouldContainJsonKeyValue(
                        "informatieobjectTypeOmschrijving",
                        INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
                    )
                    shouldContainJsonKey("informatieobjectTypeUUID")
                    shouldContainJsonKey("identificatie")
                    shouldContainJsonKeyValue("bestandsnaam", TEST_TXT_FILE_NAME)
                    shouldContainJsonKeyValue("bestandsomvang", TEST_TXT_FILE_SIZE)
                    shouldContainJsonKeyValue("formaat", TEXT_MIME_TYPE)
                }
            }
        }
        When("ondertekenInformatieObject endpoint is called") {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject" +
                    "/$enkelvoudigInformatieObjectUUID/onderteken?zaak=$zaakProductaanvraag1Uuid"
            logger.info { "Calling $endpointUrl endpoint" }

            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                requestBody = "".toRequestBody()
            )
            Then(
                "the response should be OK"
            ) {
                logger.info { "$endpointUrl status code: ${response.code}" }
                response.code shouldBe HTTP_STATUS_OK
            }
        }
    }

    Given(
        "ZAC and all related Docker containers are running and a task exists"
    ) {
        When("the create enkelvoudig informatie object with file upload endpoint is called for the task") {
            val endpointUrl = "$ZAC_API_URI/informatieobjecten/informatieobject/" +
                "$zaakProductaanvraag1Uuid/$task1ID?taakObject=true"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_PDF_FILE_NAME).let {
                File(it!!.path)
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_PDF_FILE_NAME)
                    .addFormDataPart("titel", fileTitle)
                    .addFormDataPart("bestandsomvang", file.length().toString())
                    .addFormDataPart("formaat", PDF_MIME_TYPE)
                    .addFormDataPart(
                        "file",
                        TEST_PDF_FILE_NAME,
                        file.asRequestBody(PDF_MIME_TYPE.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .build()
            val response = itestHttpClient.performPostRequest(
                url = endpointUrl,
                headers = Headers.headersOf(
                    "Accept",
                    "application/json",
                    "Content-Type",
                    "multipart/form-data"
                ),
                requestBody = requestBody
            )
            Then(
                "the response should be OK and contain information for the created document and uploaded file"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("auteur", TEST_USER_1_NAME)
                    shouldContainJsonKeyValue("beschrijving", "taak-document")
                    shouldContainJsonKeyValue("bestandsnaam", TEST_PDF_FILE_NAME)
                    shouldContainJsonKeyValue("bestandsomvang", TEST_PDF_FILE_SIZE)
                    shouldContainJsonKeyValue(
                        "creatiedatum",
                        LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    )
                    shouldContainJsonKeyValue("formaat", PDF_MIME_TYPE)
                    shouldContainJsonKey("identificatie")
                    shouldContainJsonKeyValue(
                        "informatieobjectTypeOmschrijving",
                        INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
                    )
                    shouldContainJsonKeyValue(
                        "informatieobjectTypeUUID",
                        INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
                    )
                    shouldContainJsonKeyValue("isBesluitDocument", false)
                    // a document added to a task should _always_ have the status 'definitief'
                    shouldContainJsonKeyValue("status", documentStatusDefinitief)
                    shouldContainJsonKeyValue("taal", "Nederlands")
                    shouldContainJsonKeyValue("titel", fileTitle)
                    shouldContainJsonKeyValue("versie", 1)
                    shouldContainJsonKey("uuid")
                    shouldContainJsonKeyValue(
                        "vertrouwelijkheidaanduiding",
                        documentVertrouwelijkheidsAanduidingOpenbaar
                    )
                }
            }
        }
    }
})
