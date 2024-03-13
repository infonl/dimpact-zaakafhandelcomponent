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
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.SMARTDOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED
import nl.lifely.zac.itest.config.ItestConfiguration.USER_FULL_NAME
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * This test assumes a zaak has been created, and a task has been started in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED)
class InformatieObjectenTest : BehaviorSpec() {
    companion object {
        const val PDF_FILE_NAME = "dummyTestDocument.pdf"
        const val PDF_FILE_SIZE = 9268
        const val PDF_FILE_FORMAAT = "application/pdf"
        const val TXT_FILE_NAME = "testTextDocument.txt"
        const val TXT_FILE_SIZE = 63
        const val TXT_FILE_FORMAAT = "application/text"
        const val FILE_TITLE = "dummyTitel"
        const val UPDATED_FILE_TITLE = "updated title"
        const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK = "zaakvertrouwelijk"
        const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR = "openbaar"
        const val DOCUMENT_STATUS_DEFINITIEF = "definitief"
        const val DOCUMENT_STATUS_IN_BEWERKING = "in_bewerking"
    }

    private val logger = KotlinLogging.logger {}
    private val itestHttpClient = ItestHttpClient()

    private lateinit var enkelvoudigInformatieObjectUUID: String

    init {
        Given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the create document informatie objecten endpoint is called") {
                val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/documentcreatie"
                logger.info { "Calling $endpointUrl endpoint" }

                val response = itestHttpClient.performJSONPostRequest(
                    url = endpointUrl,
                    requestBodyAsString = JSONObject(
                        mapOf(
                            "zaakUUID" to zaak1UUID
                        )
                    ).toString()
                )
                Then(
                    "the response should be OK and the response should contain a redirect URL to Smartdocuments"
                ) {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpStatusCode.OK_200.code()

                    with(responseBody) {
                        shouldContainJsonKeyValue(
                            "redirectURL",
                            "$SMARTDOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=dummySmartdocumentsTicketID"
                        )
                    }
                }
            }
        }
        Given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the upload file endpoint is called for a zaak") {
                val file = Thread.currentThread().contextClassLoader.getResource(PDF_FILE_NAME).let {
                    File(it!!.path)
                }
                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("filename", PDF_FILE_NAME)
                        .addFormDataPart("filesize", file.length().toString())
                        .addFormDataPart("type", PDF_FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            PDF_FILE_NAME,
                            file.asRequestBody(PDF_FILE_FORMAAT.toMediaType())
                        )
                        .build()
                val response = itestHttpClient.performPostRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/upload/$zaak1UUID",
                    requestBody = requestBody
                )
                Then(
                    "the response should be OK"
                ) {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpStatusCode.OK_200.code()
                }
            }
        }
        Given(
            "A zaak exists and a file has been uploaded"
        ) {
            When("the create enkelvoudig informatie object endpoint is called for the zaak") {
                val endpointUrl =
                    "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/$zaak1UUID/$zaak1UUID"
                logger.info { "Calling $endpointUrl endpoint" }
                val createDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm+01:00").format(ZonedDateTime.now())
                val postBody = "{\n" +
                    "\"bestandsnaam\":\"$PDF_FILE_NAME\",\n" +
                    "\"titel\":\"$FILE_TITLE\",\n" +
                    "\"informatieobjectTypeUUID\":\"$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID\",\n" +
                    "\"vertrouwelijkheidaanduiding\":\"$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK\",\n" +
                    "\"status\":\"$DOCUMENT_STATUS_IN_BEWERKING\",\n" +
                    "\"creatiedatum\":\"${createDate}\",\n" +
                    "\"auteur\":\"$USER_FULL_NAME\",\n" +
                    "\"taal\":\"dut\"\n" +
                    "}"
                val response = itestHttpClient.performJSONPostRequest(
                    url = endpointUrl,
                    requestBodyAsString = postBody
                )
                Then(
                    "the response should be OK and should contain information about the created document"
                ) {
                    val responseBody = response.body!!.string()
                    logger.info { "$endpointUrl response: $responseBody" }
                    response.code shouldBe HttpStatusCode.OK_200.code()
                    with(responseBody) {
                        shouldContainJsonKeyValue("auteur", USER_FULL_NAME)
                        shouldContainJsonKeyValue("bestandsnaam", PDF_FILE_NAME)
                        shouldContainJsonKeyValue("status", DOCUMENT_STATUS_IN_BEWERKING)
                        shouldContainJsonKeyValue("taal", "Nederlands")
                        shouldContainJsonKeyValue("titel", FILE_TITLE)
                        shouldContainJsonKeyValue(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                        )
                        shouldContainJsonKeyValue("formaat", PDF_FILE_FORMAAT)
                        shouldContainJsonKeyValue(
                            "informatieobjectTypeOmschrijving",
                            INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
                        )
                        shouldContainJsonKey("informatieobjectTypeUUID")
                        shouldContainJsonKey("identificatie")
                    }
                }
            }
        }
        Given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the create enkelvoudig informatie object with file upload endpoint is called for the zaak") {
                val endpointUrl =
                    "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/$zaak1UUID/$zaak1UUID"
                logger.info { "Calling $endpointUrl endpoint" }
                val file = Thread.currentThread().contextClassLoader.getResource(PDF_FILE_NAME).let {
                    File(it!!.path)
                }
                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("bestandsnaam", PDF_FILE_NAME)
                        .addFormDataPart("titel", FILE_TITLE)
                        .addFormDataPart("bestandsomvang", file.length().toString())
                        .addFormDataPart("formaat", PDF_FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            PDF_FILE_NAME,
                            file.asRequestBody(PDF_FILE_FORMAAT.toMediaType())
                        )
                        .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                        .addFormDataPart(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                        )
                        .addFormDataPart("status", DOCUMENT_STATUS_IN_BEWERKING)
                        .addFormDataPart(
                            "creatiedatum",
                            DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd'T'hh:mm+01:00"
                            ).format(ZonedDateTime.now())
                        )
                        .addFormDataPart("auteur", USER_FULL_NAME)
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
                    response.code shouldBe HttpStatusCode.OK_200.code()
                    with(responseBody) {
                        shouldContainJsonKeyValue("auteur", USER_FULL_NAME)
                        shouldContainJsonKeyValue("status", DOCUMENT_STATUS_IN_BEWERKING)
                        shouldContainJsonKeyValue("taal", "Nederlands")
                        shouldContainJsonKeyValue("titel", FILE_TITLE)
                        shouldContainJsonKeyValue(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                        )
                        shouldContainJsonKeyValue(
                            "informatieobjectTypeOmschrijving",
                            INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
                        )
                        shouldContainJsonKey("informatieobjectTypeUUID")
                        shouldContainJsonKey("identificatie")
                        shouldContainJsonKeyValue("bestandsnaam", PDF_FILE_NAME)
                        shouldContainJsonKeyValue("bestandsomvang", file.length().toString())
                        shouldContainJsonKeyValue("formaat", PDF_FILE_FORMAAT)
                    }

                    val enkelvoudigInformatieObjectAsJSON = JSONObject(responseBody)
                    enkelvoudigInformatieObjectUUID = enkelvoudigInformatieObjectAsJSON.getString("uuid")
                }
            }
            When("update of enkelvoudig informatie object with file upload endpoint is called") {
                val endpointUrl =
                    "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/update"
                logger.info { "Calling $endpointUrl endpoint" }
                val file = Thread.currentThread().contextClassLoader.getResource(TXT_FILE_NAME).let {
                    File(it!!.path)
                }

                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("uuid", enkelvoudigInformatieObjectUUID)
                        .addFormDataPart("zaakUuid", zaak1UUID.toString())
                        .addFormDataPart("bestandsnaam", TXT_FILE_NAME)
                        .addFormDataPart("titel", UPDATED_FILE_TITLE)
                        .addFormDataPart("bestandsomvang", TXT_FILE_SIZE.toString())
                        .addFormDataPart("formaat", TXT_FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            TXT_FILE_NAME,
                            file.asRequestBody(TXT_FILE_FORMAAT.toMediaType())
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
                    response.code shouldBe HttpStatusCode.OK_200.code()
                    with(responseBody) {
                        shouldContainJsonKeyValue("auteur", USER_FULL_NAME)
                        shouldContainJsonKeyValue("status", DOCUMENT_STATUS_IN_BEWERKING)
                        shouldContainJsonKeyValue("taal", "Nederlands")
                        shouldContainJsonKeyValue("titel", UPDATED_FILE_TITLE)
                        shouldContainJsonKeyValue(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                        )
                        shouldContainJsonKeyValue(
                            "informatieobjectTypeOmschrijving",
                            INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
                        )
                        shouldContainJsonKey("informatieobjectTypeUUID")
                        shouldContainJsonKey("identificatie")
                        shouldContainJsonKeyValue("bestandsnaam", TXT_FILE_NAME)
                        shouldContainJsonKeyValue("bestandsomvang", TXT_FILE_SIZE)
                        shouldContainJsonKeyValue("formaat", TXT_FILE_FORMAAT)
                    }
                }
            }
        }

        Given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the upload file endpoint is called for a task") {
                val file = Thread.currentThread().contextClassLoader.getResource("dummyTestDocument.pdf").let {
                    File(it!!.path)
                }
                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("filename", PDF_FILE_NAME)
                        .addFormDataPart("filesize", file.length().toString())
                        .addFormDataPart("type", PDF_FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            PDF_FILE_NAME,
                            file.asRequestBody("application/pdf".toMediaType())
                        )
                        .build()
                val response = itestHttpClient.performPostRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/upload/$task1ID",
                    requestBody = requestBody
                )
                Then(
                    "the response should be OK"
                ) {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpStatusCode.OK_200.code()
                }
            }
        }
        Given(
            "A task exists for a zaak and a file has been uploaded for the task"
        ) {
            When("the create enkelvoudig informatie object endpoint is called for the task") {
                val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/" +
                    "$zaak1UUID/$task1ID?taakObject=true"
                logger.info { "Calling $endpointUrl endpoint" }
                val postBody = "{\n" +
                    "\"bestandsnaam\":\"$PDF_FILE_NAME\",\n" +
                    "\"titel\":\"$FILE_TITLE\",\n" +
                    "\"informatieobjectTypeUUID\":\"$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID\"\n" +
                    "}"
                val response = itestHttpClient.performJSONPostRequest(
                    url = endpointUrl,
                    requestBodyAsString = postBody
                )
                Then(
                    "the response should be OK and should contain information about the created document"
                ) {
                    val responseBody = response.body!!.string()
                    logger.info { "$endpointUrl response: $responseBody" }
                    response.code shouldBe HttpStatusCode.OK_200.code()
                    with(responseBody) {
                        shouldContainJsonKeyValue("auteur", USER_FULL_NAME)
                        shouldContainJsonKeyValue("beschrijving", "taak-document")
                        shouldContainJsonKeyValue("bestandsnaam", PDF_FILE_NAME)
                        shouldContainJsonKeyValue("bestandsomvang", PDF_FILE_SIZE)
                        shouldContainJsonKeyValue(
                            "creatiedatum",
                            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        )
                        shouldContainJsonKeyValue("formaat", PDF_FILE_FORMAAT)
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
                        shouldContainJsonKeyValue("status", DOCUMENT_STATUS_DEFINITIEF)
                        shouldContainJsonKeyValue("taal", "Nederlands")
                        shouldContainJsonKeyValue("titel", FILE_TITLE)
                        shouldContainJsonKeyValue("versie", 1)
                        shouldContainJsonKey("uuid")
                        shouldContainJsonKeyValue(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
                        )
                    }
                }
            }
        }
        Given(
            "ZAC and all related Docker containers are running and a task exists"
        ) {
            When("the create enkelvoudig informatie object with file upload endpoint is called for the task") {
                val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/" +
                    "$zaak1UUID/$task1ID?taakObject=true"
                logger.info { "Calling $endpointUrl endpoint" }
                val file = Thread.currentThread().contextClassLoader.getResource(PDF_FILE_NAME).let {
                    File(it!!.path)
                }
                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("bestandsnaam", PDF_FILE_NAME)
                        .addFormDataPart("titel", FILE_TITLE)
                        .addFormDataPart("bestandsomvang", file.length().toString())
                        .addFormDataPart("formaat", PDF_FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            PDF_FILE_NAME,
                            file.asRequestBody(PDF_FILE_FORMAAT.toMediaType())
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
                    response.code shouldBe HttpStatusCode.OK_200.code()
                    with(responseBody) {
                        shouldContainJsonKeyValue("auteur", USER_FULL_NAME)
                        shouldContainJsonKeyValue("beschrijving", "taak-document")
                        shouldContainJsonKeyValue("bestandsnaam", PDF_FILE_NAME)
                        shouldContainJsonKeyValue("bestandsomvang", PDF_FILE_SIZE)
                        shouldContainJsonKeyValue(
                            "creatiedatum",
                            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        )
                        shouldContainJsonKeyValue("formaat", PDF_FILE_FORMAAT)
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
                        shouldContainJsonKeyValue("status", DOCUMENT_STATUS_DEFINITIEF)
                        shouldContainJsonKeyValue("taal", "Nederlands")
                        shouldContainJsonKeyValue("titel", FILE_TITLE)
                        shouldContainJsonKeyValue("versie", 1)
                        shouldContainJsonKey("uuid")
                        shouldContainJsonKeyValue(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
                        )
                    }

                    val enkelvoudigInformatieObjectAsJSON = JSONObject(responseBody)
                    enkelvoudigInformatieObjectUUID = enkelvoudigInformatieObjectAsJSON.getString("uuid")
                }
            }
        }
    }
}
