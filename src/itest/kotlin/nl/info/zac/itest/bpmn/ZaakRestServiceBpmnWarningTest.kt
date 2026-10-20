/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_BPMN_WAARSCHUWING_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONArray
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

class ZaakRestServiceBpmnWarningTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)

    fun storeBpmnTest1Configuration(fatalDateWarningWindow: Int?) = zacClient.createZaaktypeBpmnConfiguration(
        zaakTypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
        zaakTypeDescription = ZAAKTYPE_BPMN_TEST_1_DESCRIPTION,
        bpmnProcessDefinitionKey = BPMN_TEST_PROCESS_DEFINITION_KEY,
        productaanvraagType = ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE,
        defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
        defaultBehandelaarId = BEHANDELAAR_1.username,
        nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID,
        fatalDateWarningWindow = fatalDateWarningWindow,
        testUser = BEHEERDER_1
    )

    fun storeZaakVerlopendMailSetting(isMailEnabled: Boolean) = itestHttpClient.performPutRequest(
        url = "$ZAC_API_URI/signaleringen/instellingen",
        headers = Headers.headersOf("Content-Type", "application/json"),
        requestBodyAsString = """{ "isMailEnabled": $isMailEnabled, "subjecttype": "ZAAK", "type": "ZAAK_VERLOPEND" }""",
        testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1
    )

    afterSpec {
        storeBpmnTest1Configuration(fatalDateWarningWindow = null).code shouldBe HTTP_OK
        storeZaakVerlopendMailSetting(isMailEnabled = false).code shouldBe HTTP_OK
    }

    given(
        """
        a BPMN zaaktype with a fatal date warning window of 2 days, a zaak of that zaaktype that is assigned to
        myself and whose fatal date is tomorrow, and a mail on a zaak that nears its fatal date switched on for myself
        """
    ) {
        storeZaakVerlopendMailSetting(isMailEnabled = true).code shouldBe HTTP_OK
        storeBpmnTest1Configuration(fatalDateWarningWindow = 2).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
        }
        val (zaakIdentification, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
            startDate = ZonedDateTime.now().minusDays(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1,
            behandelaarId = BEHANDELAAR_BPMN_WAARSCHUWING_1.username,
            behandelaarName = BEHANDELAAR_BPMN_WAARSCHUWING_1.displayName
        )
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
            requestBodyAsString = """
                {
                    "zaak": {
                        "uiterlijkeEinddatumAfdoening": "${ZonedDateTime.now().plusDays(1)}"
                    },
                    "reden": "fakeReason"
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1
        ).code shouldBe HTTP_OK

        `when`("I list my zaak warnings") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/waarschuwing",
                testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1
            )

            then("the BPMN zaak is listed, because the warning window of its zaaktype applies to BPMN zaken too") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                JSONArray(responseBody).let { zaken ->
                    zaken.length() shouldBe 1
                    zaken.getJSONObject(0).getString("identificatie") shouldBe zaakIdentification
                }
            }
        }

        `when`("the signaleringen are sent") {
            val sendSignaleringenResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/signaleringen/send-signaleringen",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )

            then("I get the due-date signalering of the BPMN zaak by mail") {
                sendSignaleringenResponse.code shouldBe HTTP_OK
                lateinit var receivedMails: JSONArray
                eventually(10.seconds) {
                    val receivedMailsResponse = itestHttpClient.performGetRequest(
                        url = "$GREENMAIL_API_URI/user/${BEHANDELAAR_BPMN_WAARSCHUWING_1.email}/messages/",
                        testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1
                    )
                    receivedMailsResponse.code shouldBe HTTP_OK
                    receivedMails = JSONArray(receivedMailsResponse.bodyAsString)
                    receivedMails.length() shouldBe 1
                }
                receivedMails.getJSONObject(0).getString("subject") shouldContain zaakIdentification
            }
        }
    }
})
