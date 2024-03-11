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
        const val FILE_NAME = "dummyTestDocument.pdf"
        const val FILE_SIZE = 9268
        const val FILE_TITLE = "dummyTitel"
        const val FILE_FORMAAT = "application/pdf"
        const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK = "zaakvertrouwelijk"
        const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR = "openbaar"
        const val DOCUMENT_STATUS_DEFINITIEF = "definitief"
        const val DOCUMENT_STATUS_IN_BEWERKING = "in_bewerking"
    }

    private val logger = KotlinLogging.logger {}
    private val itestHttpClient = ItestHttpClient()

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
                val file = Thread.currentThread().getContextClassLoader().getResource("dummyTestDocument.pdf").let {
                    File(it!!.path)
                }
                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("filename", FILE_NAME)
                        .addFormDataPart("filesize", file.length().toString())
                        .addFormDataPart("type", FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            FILE_NAME,
                            file.asRequestBody("application/pdf".toMediaType())
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
                val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/$zaak1UUID/$zaak1UUID"
                logger.info { "Calling $endpointUrl endpoint" }
                val postBody = "{\n" +
                    "\"bestandsnaam\":\"$FILE_NAME\",\n" +
                    "\"titel\":\"$FILE_TITLE\",\n" +
                    "\"informatieobjectTypeUUID\":\"$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID\",\n" +
                    "\"vertrouwelijkheidaanduiding\":\"$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK\",\n" +
                    "\"status\":\"$DOCUMENT_STATUS_IN_BEWERKING\",\n" +
                    "\"creatiedatum\":\"${DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm+01:00").format(ZonedDateTime.now())}\",\n" +
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
                        shouldContainJsonKeyValue("bestandsnaam", FILE_NAME)
                        shouldContainJsonKeyValue("status", DOCUMENT_STATUS_IN_BEWERKING)
                        shouldContainJsonKeyValue("taal", "Nederlands")
                        shouldContainJsonKeyValue("titel", FILE_TITLE)
                        shouldContainJsonKeyValue(
                            "vertrouwelijkheidaanduiding",
                            DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
                        )
                        shouldContainJsonKeyValue("formaat", FILE_FORMAAT)
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
            When("the upload file endpoint is called for a task") {
                val file = Thread.currentThread().getContextClassLoader().getResource("dummyTestDocument.pdf").let {
                    File(it!!.path)
                }
                val requestBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("filename", FILE_NAME)
                        .addFormDataPart("filesize", file.length().toString())
                        .addFormDataPart("type", FILE_FORMAAT)
                        .addFormDataPart(
                            "file",
                            FILE_NAME,
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
                    "\"bestandsnaam\":\"$FILE_NAME\",\n" +
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
                        shouldContainJsonKeyValue("bestandsnaam", FILE_NAME)
                        shouldContainJsonKeyValue("bestandsomvang", FILE_SIZE)
                        shouldContainJsonKeyValue(
                            "creatiedatum",
                            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        )
                        shouldContainJsonKeyValue("formaat", FILE_FORMAAT)
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
    }
}
