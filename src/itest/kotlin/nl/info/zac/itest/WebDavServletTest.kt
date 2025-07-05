/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.MediaType
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_WORD_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.WORD_DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLDecoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * This test creates a zaak and uploads a document and because we do not want this test
 * to impact e.g. [SearchRestServiceTest] we run it afterward.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
class WebDavServletTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val zacClient = ZacClient()
    val itestHttpClient = ItestHttpClient()
    lateinit var enkelvoudigInformatieObjectUUID: String
    lateinit var wordDocumentWebDAVToken: UUID

    Given("A zaak that was created") {
        lateinit var zaakUUID: UUID
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            behandelaarId = TEST_USER_2_ID,
            startDate = DATE_TIME_2024_01_31
        ).run {
            val responseBody = body.string()
            logger.info { "Response: $responseBody" }
            this.isSuccessful shouldBe true
            JSONObject(responseBody).run {
                zaakUUID = getString("uuid").run(UUID::fromString)
            }
        }
        When(
            """
                the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a DOCX file
                """
        ) {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUUID/$zaakUUID"
            logger.info { "Calling $endpointUrl endpoint" }
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_WORD_FILE_NAME).let {
                File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
            }
            val requestBody =
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("bestandsnaam", TEST_WORD_FILE_NAME)
                    .addFormDataPart("titel", WORD_DOCUMENT_FILE_TITLE)
                    .addFormDataPart("bestandsomvang", file.length().toString())
                    .addFormDataPart("formaat", MediaType.DOCX.toMediaType().toString())
                    .addFormDataPart(
                        "file",
                        TEST_WORD_FILE_NAME,
                        file.asRequestBody(MediaType.DOCX.toMediaType())
                    )
                    .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                    .addFormDataPart(
                        "vertrouwelijkheidaanduiding",
                        DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
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
            Then(
                "the response should be OK and contain information for the created Word document and uploaded file"
            ) {
                val responseBody = response.body.string()
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_OK
                enkelvoudigInformatieObjectUUID = JSONObject(responseBody).getString("uuid")
            }
        }
        When("the edit endpoint is called on the enkelvoudig informatie object") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/informatieobjecten/informatieobject/$enkelvoudigInformatieObjectUUID/edit" +
                    "?zaak=$zaakUUID"
            )
            Then(
                """
                the response should be ok and should contain an MS Word WebDAV redirect URL with a
                token for the uploaded Word document
                """
            ) {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody shouldStartWith "\"ms-word:http://localhost:8080/webdav/folder/"
                responseBody shouldEndWith ".docx\""
                wordDocumentWebDAVToken = UUID.fromString(responseBody.substringAfterLast("/").substringBefore(".docx"))
            }
        }

        When("the DOCX Word document is requested using the WebDAV token for the uploaded file") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_BASE_URI/webdav/folder/$wordDocumentWebDAVToken.docx",
                // the WebDAV servlet does not require any authorization
                addAuthorizationHeader = false
            )

            Then("the response should be ok (and contain the DOCX Word document)") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }

        When("a HEAD request is performed using the WebDAV token for the uploaded file") {
            val response = itestHttpClient.performHeadRequest(
                url = "$ZAC_BASE_URI/webdav/folder/$wordDocumentWebDAVToken.docx",
                // the WebDAV servlet does not require any authorization
                addAuthorizationHeader = false
            )

            Then("the response should be ok") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }
    }
})
