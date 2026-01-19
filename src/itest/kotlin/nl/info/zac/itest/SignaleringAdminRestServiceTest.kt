/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_GEMEENTE_EMAIL_ADDRESS
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_DESCRIPTION_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class SignaleringAdminRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given(
        """
            A user who has 'taak verlopen email notificaties' turned on 
            and a zaak with a task that is assigned and that has a fatal/due date within one day from the zaak start date,
            and a behandelaar is logged in
            """
    ) {
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)
        val response = itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/signaleringen/instellingen",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json"
            ),
            requestBodyAsString = """{ "mail": true, "subjecttype": "TAAK", "type": "TAAK_VERLOPEN" }""",
            addAuthorizationHeader = true
        )
        response.code shouldBe HTTP_OK

        lateinit var zaakUuid: UUID
        lateinit var zaakIdentification: String
        zacClient.createZaak(
            description = ZAAK_DESCRIPTION_1,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2024_01_01,
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID
        ).run {
            JSONObject(bodyAsString).run {
                zaakIdentification = getString("identificatie")
                zaakUuid = getString("uuid").run(UUID::fromString)
            }
        }

        val getHumanTaskPlanItemsResponse = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUuid/humanTaskPlanItems"
        )
        val getHumanTaskPlanItemsResponseBody = getHumanTaskPlanItemsResponse.bodyAsString
        logger.info { "Response: $getHumanTaskPlanItemsResponseBody" }
        getHumanTaskPlanItemsResponse.code shouldBe HTTP_OK
        getHumanTaskPlanItemsResponseBody.shouldBeJsonArray()
        val humanTaskItemId = JSONArray(getHumanTaskPlanItemsResponseBody).getJSONObject(0).getString("id")
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        val fataleDatum = DATE_2024_01_01.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        // start a task and assign it to the currently logged-in user (i.e. 'myself')
        val doHumanTaskPlanItemResponse = itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
            requestBodyAsString = """
                {
                    "planItemInstanceId": "$humanTaskItemId",
                    "fataledatum": "$fataleDatum",
                    "taakStuurGegevens": { "sendMail": false },
                    "medewerker": {
                        "id": "${BEHANDELAAR_DOMAIN_TEST_1.username}",
                        "naam": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}"
                    },
                    "groep": {
                        "id": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                    },
                    "taakdata":{}
                }
            """.trimIndent()

        )

        val doHumanTaskPlanItemResponseBody = doHumanTaskPlanItemResponse.bodyAsString
        logger.info { "Start task response: $doHumanTaskPlanItemResponseBody" }
        doHumanTaskPlanItemResponse.code shouldBe HTTP_NO_CONTENT

        When("The internal endpoint to send signaleringen is called with a valid API key") {
            val sendSignaleringenResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/signaleringen/send-signaleringen",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders(),
                addAuthorizationHeader = false
            )

            Then("the response should be 'ok' and a task signalering email should be sent") {
                sendSignaleringenResponse.code shouldBe HTTP_OK
                val sendSignaleringenResponseBody = sendSignaleringenResponse.bodyAsString
                logger.info { "Response: $sendSignaleringenResponseBody" }
                sendSignaleringenResponseBody shouldBe "Started sending signaleringen using job: 'Signaleringen verzenden'"

                // give it some time until the signaleringen mail has been sent by ZAC and received by Greenmail
                lateinit var receivedMails: JSONArray
                eventually(5.seconds) {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/${BEHANDELAAR_DOMAIN_TEST_1.email}/messages/"
                    )
                    receivedMailsResponse.code shouldBe HTTP_OK
                    receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    receivedMails.length() shouldBe 1
                }
                with(JSONArray(receivedMails).getJSONObject(0)) {
                    getString("subject") shouldBe "Actie nodig, handel jouw taak voor zaak $zaakIdentification spoedig af"
                    with(getString("mimeMessage")) {
                        this shouldStartWith "Return-Path: <$TEST_GEMEENTE_EMAIL_ADDRESS>"
                        this shouldContain
                            "Voor zaak $zaakIdentification over $ZAAK_DESCRIPTION_1 staat een belangrijke een taak op jouw naam. " +
                            "De fatale datum voor het afhandelen is verstreken."
                    }
                }
            }
        }
    }
})
