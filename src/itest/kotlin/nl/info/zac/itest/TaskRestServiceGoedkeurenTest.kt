/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_3_IDENTIFICATION
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.enkelvoudigInformatieObjectUUID
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, then adds a document, starts the 'Goedkeuren' task
 * and completes this task by approving the document.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
@Suppress("MagicNumber")
class TaskRestServiceGoedkeurenTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak has been created that has finished the intake phase with the status 'admissible'") {
        lateinit var zaakUUID: UUID
        lateinit var enkelvoudigInformatieObjectUUID: UUID
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            JSONObject(body.string()).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems"
        ).run {
            JSONArray(body.string()).getJSONObject(0).run {
                intakeId = getString("id").toInt()
            }
        }
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
            {
                "zaakUuid":"$zaakUUID",
                "planItemInstanceId":"$intakeId",
                "actie":"$ACTIE_INTAKE_AFRONDEN",
                "zaakOntvankelijk":true
            }
            """.trimIndent()
        ).run {
            logger.info { "Response: ${body.string()}" }
            code shouldBe HTTP_NO_CONTENT
        }
        When(
            """
            the create enkelvoudig informatie object with file upload endpoint is called for the zaak with a TXT file
            """
        ) {
            val endpointUrl =
                "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUUID/$zaakUUID"
            val file = Thread.currentThread().contextClassLoader.getResource(TEST_TXT_FILE_NAME).let {
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

            Then("the response should be OK and contain information for the created document") {
                val responseBody = response.body.string()
                logger.info { "$endpointUrl response: $responseBody" }
                response.code shouldBe HTTP_OK
                enkelvoudigInformatieObjectUUID = UUID.fromString(JSONObject(responseBody).getString("uuid"))
            }
        }
        // TODO: start the 'Goedkeuren' task
        // TODO: complete the 'Goedkeuren' task by approving the document
    }
})
