/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_1_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_2_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_DEFINITIEF
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_UPDATED_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_SIZE
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_CONVERTED_TO_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_SIZE
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.info.zac.itest.config.ItestConfiguration.task1ID
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * This test assumes a zaak has been created, and a task has been started in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED)
class EnkelvoudigInformatieObjectRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    lateinit var enkelvoudigInformatieObject2UUID: String

    Given(
        "ZAC and all related Docker containers are running and zaak exists"
    ) {
        When(
            """
            the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a PDF file
            """
        ) {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakProductaanvraag1Uuid/$zaakProductaanvraag1Uuid"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_PDF_FILE_NAME)?.let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_PDF_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_FILE_TITLE)
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
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                    )
                    .addFormDataPart("status", DOCUMENT_STATUS_IN_BEWERKING)
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

            Then("the response should be OK and contain information for the created document and uploaded file") {
                val responseBody = response.body!!.string()
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                         {
                          "bestandsnaam" : "$TEST_PDF_FILE_NAME",
                          "auteur" : "$TEST_USER_1_NAME",
                          "beschrijving" : "",
                          "bestandsomvang" : ${file.length()},
                          "creatiedatum" : "${LocalDate.now()}",
                          "formaat" : "$PDF_MIME_TYPE",
                          "identificatie" : "$DOCUMENT_1_IDENTIFICATION",
                          "indicatieGebruiksrecht" : false,
                          "indicaties" : [ ],
                          "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                          "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                          "isBesluitDocument" : false,
                          "link" : "",
                          "rechten" : {
                            "lezen" : true,
                            "ondertekenen" : true,
                            "ontgrendelen" : true,
                            "toevoegenNieuweVersie" : true,
                            "vergrendelen" : true,
                            "verwijderen" : true,
                            "wijzigen" : true
                          },
                          "status" : "$DOCUMENT_STATUS_IN_BEWERKING",
                          "taal" : "Nederlands",
                          "titel" : "$DOCUMENT_FILE_TITLE",
                          "versie" : 1,
                          "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK"
                        }
                """.trimIndent()
                enkelvoudigInformatieObjectUUID = JSONObject(responseBody).getString("uuid")
            }
        }
        When("update of enkelvoudig informatie object with file upload endpoint is called with a TXT file") {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/update"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_TXT_FILE_NAME)?.let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }

            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("uuid", enkelvoudigInformatieObjectUUID)
                    .addFormDataPart("zaakUuid", zaakProductaanvraag1Uuid.toString())
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart("bestandsnaam", TEST_TXT_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_UPDATED_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", TEST_TXT_FILE_SIZE.toString())
                    .addFormDataPart("formaat", TEXT_MIME_TYPE)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                    )
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
                    shouldContainJsonKeyValue("status", DOCUMENT_STATUS_IN_BEWERKING)
                    shouldContainJsonKeyValue("taal", "Nederlands")
                    shouldContainJsonKeyValue("titel", DOCUMENT_UPDATED_FILE_TITLE)
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
        When("the huidigeversie endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUUID/huidigeversie"
            )
            Then(
                """
                    the response should be OK and the informatieobject should be returned and
                    should have the status 'definitief' since it was signed
                    """
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("auteur", TEST_USER_1_NAME)
                    shouldContainJsonKeyValue("status", DOCUMENT_STATUS_DEFINITIEF)
                    shouldContainJsonKeyValue("titel", DOCUMENT_UPDATED_FILE_TITLE)
                    shouldContainJsonKeyValue(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                    )
                    shouldContainJsonKey("informatieobjectTypeUUID")
                    shouldContainJsonKey("bestandsnaam")
                }
            }
        }
        When(
            """
                the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a TXT file
                """
        ) {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakProductaanvraag1Uuid/$zaakProductaanvraag1Uuid"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_TXT_FILE_NAME)?.let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_TXT_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", file.length().toString())
                    .addFormDataPart("formaat", TEXT_MIME_TYPE)
                    .addFormDataPart(
                        "file",
                        TEST_TXT_FILE_NAME,
                        file.asRequestBody(TEXT_MIME_TYPE.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
                    )
                    .addFormDataPart("status", DOCUMENT_STATUS_DEFINITIEF)
                    .addFormDataPart(
                        "creatiedatum",
                        DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd'T'HH:mm+01:00"
                        ).format(ZonedDateTime.now())
                    )
                    .addFormDataPart("auteur", TEST_USER_1_NAME)
                    .addFormDataPart("taal", "eng")
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
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                      "bestandsnaam" : "$TEST_TXT_FILE_NAME",
                      "auteur" : "$TEST_USER_1_NAME",
                      "beschrijving" : "",
                      "bestandsomvang" : ${file.length()},
                      "creatiedatum" : "${LocalDate.now()}",
                      "formaat" : "$TEXT_MIME_TYPE",
                      "identificatie" : "$DOCUMENT_2_IDENTIFICATION",
                      "indicatieGebruiksrecht" : false,
                      "indicaties" : [ ],
                      "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                      "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                      "isBesluitDocument" : false,
                      "link" : "",
                      "rechten" : {
                        "lezen" : true,
                        "ondertekenen" : true,
                        "ontgrendelen" : true,
                        "toevoegenNieuweVersie" : true,
                        "vergrendelen" : true,
                        "verwijderen" : true,
                        "wijzigen" : true
                      },
                      "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                      "taal" : "Engels",
                      "titel" : "$DOCUMENT_FILE_TITLE",
                      "versie" : 1,
                      "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                    }
                """.trimIndent()
                enkelvoudigInformatieObject2UUID = JSONObject(responseBody).getString("uuid")
            }
        }
        When("the convert endpoint is called") {
            val response = itestHttpClient.performPostRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObject2UUID/" +
                    "convert?zaak=$zaakProductaanvraag1Uuid",
                requestBody = "".toRequestBody()
            )
            Then(
                """
                    the response should be OK and the informatieobject should be converted from TXT to PDF
                    """
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
            }
        }
        When("the get enkelvoudiginformatieobject endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObject2UUID/"
            )
            Then(
                """
                    the response should be OK and should contain information about the document converted to PDF
                    """
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                         {
                          "bestandsnaam" : "$TEST_TXT_CONVERTED_TO_PDF_FILE_NAME",
                          "auteur" : "$TEST_USER_1_NAME",
                          "beschrijving" : "",
                          "creatiedatum" : "${LocalDate.now()}",
                          "formaat" : "$PDF_MIME_TYPE",
                          "identificatie" : "$DOCUMENT_2_IDENTIFICATION",
                          "indicatieGebruiksrecht" : false,
                          "indicaties" : [ ],
                          "informatieobjectTypeOmschrijving" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING",
                          "informatieobjectTypeUUID" : "$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID",
                          "isBesluitDocument" : false,
                          "rechten" : {
                            "lezen" : true,
                            "ondertekenen" : false,
                            "ontgrendelen" : true,
                            "toevoegenNieuweVersie" : true,
                            "vergrendelen" : false,
                            "verwijderen" : true,
                            "wijzigen" : true
                          },
                          "status" : "$DOCUMENT_STATUS_DEFINITIEF",
                          "taal" : "Engels",
                          "titel" : "$DOCUMENT_FILE_TITLE",
                          "versie" : 2,
                          "vertrouwelijkheidaanduiding" : "$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR"
                        }
                """.trimIndent()
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
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_PDF_FILE_NAME)?.let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_PDF_FILE_NAME)
                    .addFormDataPart("titel", DOCUMENT_FILE_TITLE)
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
                    shouldContainJsonKeyValue("status", DOCUMENT_STATUS_DEFINITIEF)
                    shouldContainJsonKeyValue("taal", "Nederlands")
                    shouldContainJsonKeyValue("titel", DOCUMENT_FILE_TITLE)
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
})
