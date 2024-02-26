/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.lifely.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.SMARTDOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.USER_FULL_NAME
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * This test assumes a zaak has been created, and a task has been started in a previously run test.
 */
@Order(2)
class InformatieObjectenTest : BehaviorSpec() {
    companion object {
        const val FILE_NAME = "dummyTestDocument.pdf"
        const val FILE_TITLE = "dummyTitel"
        const val FILE_FORMAAT = "application/pdf"
        const val DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING = "zaakvertrouwelijk"
        const val DOCUMENT_STATUS = "definitief"
    }

    private val logger = KotlinLogging.logger {}
    private val zacClient: ZacClient = ZacClient()

    init {
        given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the create document informatie objecten endpoint is called") {
                then(
                    "the 'unattended document creation wizard' is started in Smartdocuments"
                ) {
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/documentcreatie"
                    logger.info { "Calling $endpointUrl endpoint" }

                    khttp.post(
                        url = endpointUrl,
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        ),
                        data = JSONObject(
                            mapOf(
                                "zaakUUID" to zaak1UUID
                            )
                        )
                    ).apply {
                        logger.info { "$endpointUrl response: $text" }

                        statusCode shouldBe HttpStatus.SC_OK
                        JSONObject(text).getString("redirectURL") shouldBe
                            "$SMARTDOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=dummySmartdocumentsTicketID"
                    }
                }
            }
        }
        given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the upload file endpoint is called") {
                then(
                    "the file is temporarily stored in ZAC"
                ) {
                    val file = Thread.currentThread().getContextClassLoader().getResource("dummyTestDocument.pdf").let {
                        File(it!!.path)
                    }
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/upload/$zaak1UUID"
                    logger.info { "Calling $endpointUrl endpoint" }

                    val request = Request.Builder()
                        .header("Authorization", "Bearer ${KeycloakClient.requestAccessToken()}")
                        .url(endpointUrl)
                        .post(
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
                        )
                        .build()

                    zacClient.okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body!!.string()
                        logger.info { "$endpointUrl response: $responseBody" }

                        response.isSuccessful shouldBe true
                    }
                }
            }
        }
        given(
            "A zaak exsits and a file has been uploaded"
        ) {
            When("the create enkelvoudig informatie object endpoint is called for the zaak") {
                then(
                    "the document is created in Open Zaak and is attached to the zaak"
                ) {
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/informatieobject/$zaak1UUID/$zaak1UUID"
                    logger.info { "Calling $endpointUrl endpoint" }

                    val postBody = "{\n" +
                        "\"bestandsnaam\":\"$FILE_NAME\",\n" +
                        "\"titel\":\"$FILE_TITLE\",\n" +
                        "\"informatieobjectTypeUUID\":\"$INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID\",\n" +
                        "\"vertrouwelijkheidaanduiding\":\"$DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING\",\n" +
                        "\"status\":\"$DOCUMENT_STATUS\",\n" +
                        "\"creatiedatum\":\"2024-02-26T11:32:04+01:00\",\n" +
                        "\"auteur\":\"$USER_FULL_NAME\",\n" +
                        "\"taal\":\"dut\"\n" +
                        "}"
                    val request = Request.Builder()
                        .header("Authorization", "Bearer ${KeycloakClient.requestAccessToken()}")
                        .header("Content-Type", "application/json")
                        .url(endpointUrl)
                        .post(postBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    zacClient.okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body!!.string()
                        logger.info { "$endpointUrl response: $responseBody" }

                        response.isSuccessful shouldBe true
                        with(responseBody) {
                            shouldContainJsonKeyValue("auteur", USER_FULL_NAME)
                            shouldContainJsonKeyValue("bestandsnaam", FILE_NAME)
                            shouldContainJsonKeyValue("status", DOCUMENT_STATUS)
                            shouldContainJsonKeyValue("taal", "Nederlands")
                            shouldContainJsonKeyValue("titel", FILE_TITLE)
                            shouldContainJsonKeyValue(
                                "vertrouwelijkheidaanduiding",
                                DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING
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
        }
    }
}
