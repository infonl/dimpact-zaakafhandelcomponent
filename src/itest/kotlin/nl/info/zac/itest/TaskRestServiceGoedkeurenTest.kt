/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_KOPPELEN
import nl.info.zac.itest.config.ItestConfiguration.TEST_TXT_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEXT_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, then adds a document, starts the 'Goedkeuren' task
 * and completes this task by signing the document.
 * Because we do not want this test to impact e.g. [SearchRestServiceTest] we run it afterward.
 */
@Order(TEST_SPEC_ORDER_AFTER_KOPPELEN)
@Suppress("MagicNumber")
class TaskRestServiceGoedkeurenTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak has been created that has finished the intake phase with the status 'admissible'") {
        val zaakUUID: UUID
        lateinit var enkelvoudigInformatieObjectUUID: UUID
        lateinit var humanTaskItemGoedkeurenId: String
        var goedkeurenTaskId: Int = 0
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

        When("the list human task plan items endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/planitems/zaak/$zaakUUID/humanTaskPlanItems"
            )
            Then("the list of human task plan items for this zaak contains the task 'aanvullende informatie'") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody.shouldBeJsonArray()
                // the zaak is in the behandelen phase, so there should be four human task plan items
                // of which the first one is 'Goedkeuren'
                JSONArray(responseBody).length() shouldBe 4
                JSONArray(responseBody)[0].toString().run {
                    shouldContainJsonKeyValue("naam", "Goedkeuren")
                }
                humanTaskItemGoedkeurenId = JSONArray(responseBody).getJSONObject(0).getString("id")
            }
        }

        When("the start human task plan item endpoint is called for the task 'Goedkeuren'") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
                requestBodyAsString = """
                {
                    "planItemInstanceId": "$humanTaskItemGoedkeurenId",
                    "groep": { "id": "$TEST_GROUP_A_ID", "naam": "$TEST_GROUP_A_DESCRIPTION" },
                    "taakStuurGegevens": {},
                    "taakdata": {
                        "vraag": "fakeQuestion",
                        "relevanteDocumenten": "$enkelvoudigInformatieObjectUUID"
                    }
                }
                """.trimIndent()
            )
            Then("a task is started for this zaak") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }

        When("the get tasks for the zaak endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/taken/zaak/$zaakUUID"
            )

            Then("the list with tasks for this zaak is returned") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                // only the 'Goedkeuren' task should be active
                goedkeurenTaskId = JSONArray(responseBody).getJSONObject(0).getString("id").toInt()
            }
        }

        When("the 'Goedkeuren' task is completed by requesting the signing of the document") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = """
                {
                    "creatiedatumTijd": "${ZonedDateTime.now()}",
                    "formulierDefinitieId": "GOEDKEUREN",
                    "groep": { "id": "$TEST_GROUP_A_ID", "naam": "$TEST_GROUP_A_DESCRIPTION" },
                    "id": "$goedkeurenTaskId",
                    "naam": "Goedkeuren",
                    "rechten":{ "lezen": true, "toekennen": true, "toevoegenDocument": true, "wijzigen": true },
                    "status": "NIET_TOEGEKEND",
                    "taakdata": {
                        "relevanteDocumenten": "$enkelvoudigInformatieObjectUUID",
                        "vraag": "fakeQuestion",
                        "ondertekenen": "$enkelvoudigInformatieObjectUUID",
                        "goedkeuren": "goedkeuren.AKKOORD"
                    },
                    "taakdocumenten": [],
                    "taakinformatie": {
                        "uitkomst": "goedkeuren.AKKOORD",
                        "opmerking": "fakeToelichting",
                        "bijlagen": ""
                    },
                    "tabellen": {},
                    "zaakUuid": "$zaakUUID",
                    "zaaktypeOmschrijving": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_DESCRIPTION",
                    "zaaktypeUUID": "$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID",
                    "toelichting": "fakeToelichting"
                }
                """.trimIndent()
            )

            Then("the taak status should be set to 'AFGEROND'") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody.shouldContainJsonKeyValue("status", "AFGEROND")
            }
        }

        // TODO: check if the document was signed and if the status was set to 'definitief'
        // PUT https://zaakafhandelcomponent-zac-dev.dimpact.lifely.nl/rest/informatieobjecten/informatieobjectenList
        // with body: {"zaakUUID":"f34c2f1b-de0b-4187-a318-877dac23acda","informatieobjectUUIDs":["6c226862-ba35-4a0d-b420-f7a8f1e814aa"]}
    }
})
