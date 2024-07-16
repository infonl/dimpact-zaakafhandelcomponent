/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class SignaleringAdminRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given("A zaak with a task that is assigned and that has a fatal/due date within one day from now") {
        lateinit var zaakUuid: UUID
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            JSONObject(body!!.string()).run {
                getJSONObject("zaakdata").run {
                    zaakUuid = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        val getHumanTaskPlanItemsResponse = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUuid/humanTaskPlanItems"
        )
        val responseBody = getHumanTaskPlanItemsResponse.body!!.string()
        logger.info { "Response: $responseBody" }
        getHumanTaskPlanItemsResponse.isSuccessful shouldBe true
        responseBody.shouldBeJsonArray()
        val humanTaskItemId = JSONArray(responseBody).getJSONObject(0).getString("id")
        val fataleDatum = LocalDate.now()
            .minusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val doHumanTaskPlanItemResponse = itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
            requestBodyAsString = """{
                        "planItemInstanceId":"$humanTaskItemId",
                        "fataledatum":"$fataleDatum",
                        "taakStuurGegevens":{"sendMail":false},
                        "medewerker":{"id":"$TEST_USER_1_USERNAME","naam":"$TEST_USER_1_NAME"},"groep":{"id":"$TEST_GROUP_A_ID","naam":"$TEST_GROUP_A_DESCRIPTION"},
                        "taakdata":{}
                    }
            """.trimIndent()
        )
        val doHumanTaskPlanItemResponseBody = doHumanTaskPlanItemResponse.body!!.string()
        logger.info { "Response: $doHumanTaskPlanItemResponseBody" }

        When("the admin endpoint to send signaleringen is called") {
            val sendSignaleringenResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/admin/signaleringen/send-signaleringen",
                headers = Headers.headersOf(
                    "Content-Type",
                    "application/json"
                ),
                // the endpoint is a system / admin endpoint currently not requiring any authentication
                addAuthorizationHeader = false
            )

            Then("the response should be 'ok' and a task signalering email should be sent") {
                sendSignaleringenResponse.code shouldBe HTTP_STATUS_OK
                val sendSignaleringenResponseBody = sendSignaleringenResponse.body!!.string()
                logger.info { "Response: $sendSignaleringenResponseBody" }
                sendSignaleringenResponseBody shouldBe "Started sending signaleringen using job: 'Signaleringen verzenden'"

                // TODO: test email sent..
            }
        }
    }
})
