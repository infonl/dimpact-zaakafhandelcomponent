/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_NAME
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(1)
class PlanItemsRESTServiceTest : BehaviorSpec() {
    companion object {
        const val FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE = "AANVULLENDE_INFORMATIE"
        const val HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM = "Aanvullende informatie"
        const val HUMAN_TASK_TYPE = "HUMAN_TASK"
    }

    private val logger = KotlinLogging.logger {}
    private val zacClient: ZacClient = ZacClient()
    private lateinit var humanTaskItemAanvullendeInformatieId: String

    init {
        given("ZAC Docker container is running and a zaak has been created") {
            When("the list human task plan items endpoint is called") {
                then(
                    "the list of human task plan items for this zaak is returned and contains the task 'aanvullende informatie'"
                ) {
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/planitems/zaak/$zaak1UUID/humanTaskPlanItems"
                    logger.info { "Calling $endpointUrl endpoint" }

                    val request = Request.Builder()
                        .headers(
                            Headers.headersOf(
                                "Authorization",
                                "Bearer ${KeycloakClient.requestAccessToken()}",
                                "Accept",
                                "application/json"
                            )
                        )
                        .url(endpointUrl)
                        .get()
                        .build()
                    zacClient.okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body!!.string()
                        logger.info { "$endpointUrl response: $responseBody" }

                        response.isSuccessful shouldBe true
                        responseBody.shouldBeJsonArray()
                        // the zaak is in the intake phase, so there should be only be one human task plan item: 'aanvullende informatie'
                        JSONArray(responseBody).length() shouldBe 1

                        with(JSONArray(responseBody)[0].toString()) {
                            shouldContainJsonKeyValue("actief", "true")
                            shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                            shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                            shouldContainJsonKeyValue("type", HUMAN_TASK_TYPE)
                            shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                            shouldContainJsonKey("id")
                        }
                        humanTaskItemAanvullendeInformatieId = JSONArray(responseBody).getJSONObject(0).getString("id")
                    }
                }
            }
        }
        given("ZAC Docker container is running and a zaak has been created") {
            When("the get human task plan item endpoint is called for the task 'aanvullende informatie'") {
                then("the human task plan item data for this task is returned") {
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/planitems/humanTaskPlanItem/$humanTaskItemAanvullendeInformatieId"
                    logger.info { "Calling $endpointUrl endpoint" }

                    val request = Request.Builder()
                        .headers(
                            Headers.headersOf(
                                "Authorization",
                                "Bearer ${KeycloakClient.requestAccessToken()}",
                                "Accept",
                                "application/json"
                            )
                        )
                        .url(endpointUrl)
                        .get()
                        .build()
                    zacClient.okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body!!.string()
                        logger.info { "$endpointUrl response: $responseBody" }

                        response.isSuccessful shouldBe true

                        with(responseBody) {
                            shouldContainJsonKeyValue("actief", "true")
                            shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                            shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                            shouldContainJsonKeyValue("type", HUMAN_TASK_TYPE)
                            shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                            shouldContainJsonKeyValue("id", humanTaskItemAanvullendeInformatieId)
                        }
                    }
                }
            }
        }
        given("ZAC Docker container is running and a zaak has been created") {
            When("the human task plan items endpoint is called") {
                then("a task is started for this zaak") {
                    val endpointUrl = "${ItestConfiguration.ZAC_API_URI}/planitems/doHumanTaskPlanItem"
                    logger.info { "Calling $endpointUrl endpoint" }
                    val fataleDatum = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                    val postBody = "{\n" +
                        "\"planItemInstanceId\":\"$humanTaskItemAanvullendeInformatieId\",\n" +
                        "\"fataledatum\":\"$fataleDatum\",\n" +
                        "\"taakStuurGegevens\":{\"sendMail\":false},\n" +
                        "\"medewerker\":null,\"groep\":{\"id\":\"$GROUP_A_ID\",\"naam\":\"$GROUP_A_NAME\"},\n" +
                        // taakdata must be present, even if it is empty
                        "\"taakdata\":{}\n" +
                        "}"
                    val request = Request.Builder()
                        .headers(
                            Headers.headersOf(
                                "Authorization",
                                "Bearer ${KeycloakClient.requestAccessToken()}",
                                "Accept",
                                "application/json"
                            )
                        )
                        .url(endpointUrl)
                        .post(postBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    zacClient.okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body!!.string()
                        logger.info { "$endpointUrl response: $responseBody" }

                        response.isSuccessful shouldBe true
                    }
                }
            }
        }
    }
}
