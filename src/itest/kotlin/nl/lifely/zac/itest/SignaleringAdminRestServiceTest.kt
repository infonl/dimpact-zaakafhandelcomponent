/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GEMEENTE_EMAIL_ADDRESS
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_EMAIL
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakManual2Identification
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class SignaleringAdminRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given(
        """
            A user who has 'taak verlopen email notificaties' turned on 
            and a zaak with a task that is assigned and that has a fatal/due date within one day from now
            """
    ) {
        val response = itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/signaleringen/instellingen",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json"
            ),
            requestBodyAsString = """{"mail":true,"subjecttype":"TAAK","type":"TAAK_VERLOPEN"}""",
            addAuthorizationHeader = true
        )
        response.code shouldBe HTTP_STATUS_OK

        val zaakDescription = "dummyDescription"
        lateinit var zaakUuid: UUID
        zacClient.createZaak(
            description = zaakDescription,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = LocalDate.now().atStartOfDay(TimeZone.getDefault().toZoneId()),
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
        ).run {
            JSONObject(body!!.string()).run {
                zaakManual2Identification = getString("identificatie")
                zaakUuid = getString("uuid").run(UUID::fromString)
            }
        }

        val getHumanTaskPlanItemsResponse = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUuid/humanTaskPlanItems"
        )
        val getHumanTaskPlanItemsResponseBody = getHumanTaskPlanItemsResponse.body!!.string()
        logger.info { "Response: $getHumanTaskPlanItemsResponseBody" }
        getHumanTaskPlanItemsResponse.isSuccessful shouldBe true
        getHumanTaskPlanItemsResponseBody.shouldBeJsonArray()
        val humanTaskItemId = JSONArray(getHumanTaskPlanItemsResponseBody).getJSONObject(0).getString("id")

        val fataleDatum = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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
        logger.info { "Start task response: $doHumanTaskPlanItemResponseBody" }

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

                // give it some time until the signaleringen mail has been sent by ZAC and received by Greenmail
                lateinit var receivedMails: JSONArray
                eventually(5.seconds) {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "http://localhost:8888/api/user/$TEST_USER_1_EMAIL/messages/"
                    )
                    receivedMailsResponse.code shouldBe HTTP_STATUS_OK
                    receivedMails = JSONArray(receivedMailsResponse.body!!.string())
                    receivedMails.length() shouldBe 1
                }
                with(JSONArray(receivedMails).getJSONObject(0)) {
                    getString("subject") shouldBe "Actie nodig, handel jouw taak voor zaak $zaakManual2Identification spoedig af"
                    with(getString("mimeMessage")) {
                        this shouldStartWith "Return-Path: <$TEST_GEMEENTE_EMAIL_ADDRESS>"
                        this shouldContain
                            "Voor zaak $zaakManual2Identification over $zaakDescription staat een belangrijke een taak op jouw naam. " +
                            "De fatale datum voor het afhandelen is verstreken."
                    }
                }
            }
        }
    }
})
