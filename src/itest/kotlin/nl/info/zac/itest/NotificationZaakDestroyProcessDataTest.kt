/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_ZAAK_AFHANDELEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.config.TestUser
import nl.info.zac.itest.util.queryZacDatabase
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * These tests cover a very hypothetical case: they send a 'zaak destroy' notification for a zaak that still
 * exists in the zaakregister. In normal operation, a 'zaak destroy' notification is only ever sent after the zaak
 * has been deleted from the zaakregister. If such a notification is sent for an existing zaak, something is very
 * wrong in the platform.
 */
class NotificationZaakDestroyProcessDataTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    fun countRows(table: String, zaakUuid: UUID) =
        queryZacDatabase("SELECT COUNT(*) FROM flowable.$table WHERE business_key_ = '$zaakUuid'").single()

    fun createZaak(zaaktypeUuid: UUID, testUser: TestUser): UUID =
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = zaaktypeUuid,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = testUser
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).getString("uuid").run(UUID::fromString)
        }

    fun doUserEventListenerPlanItem(zaakUuid: UUID, actionJson: String) {
        val planItemInstanceId = itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$zaakUuid/userEventListenerPlanItems",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
                {
                    "zaakUuid": "$zaakUuid",
                    "planItemInstanceId": "$planItemInstanceId",
                    $actionJson
                }
            """.trimIndent(),
            testUser = RECORDMANAGER_1
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
    }

    fun sendZaakDestroyNotification(zaakUuid: UUID) =
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to "zaken",
                    "resource" to "zaak",
                    "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "actie" to "destroy",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString()
        )

    given("an open zaak with a running BPMN process") {
        val zaakUuid = createZaak(ZAAKTYPE_BPMN_TEST_1_UUID, BEHANDELAAR_1)
        countRows("act_ru_execution", zaakUuid) shouldBe "1"

        `when`("the zaak destroy notification is received") {
            val response = sendZaakDestroyNotification(zaakUuid)

            then("the running and the historic BPMN process instances of the zaak are deleted") {
                response.code shouldBe HTTP_NO_CONTENT
                countRows("act_ru_execution", zaakUuid) shouldBe "0"
                countRows("act_hi_procinst", zaakUuid) shouldBe "0"
            }
        }
    }

    given("a terminated zaak whose BPMN process has ended, so that only its history is left") {
        val zaakUuid = createZaak(ZAAKTYPE_BPMN_TEST_1_UUID, BEHANDELAAR_1)
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/afbreken",
            requestBodyAsString = """{ "zaakbeeindigRedenId": "ZAAK_NIET_ONTVANKELIJK" }""",
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
        }
        countRows("act_ru_execution", zaakUuid) shouldBe "0"
        countRows("act_hi_procinst", zaakUuid) shouldBe "1"

        `when`("the zaak destroy notification is received") {
            val response = sendZaakDestroyNotification(zaakUuid)

            then("the historic BPMN process instance of the zaak is deleted") {
                response.code shouldBe HTTP_NO_CONTENT
                countRows("act_hi_procinst", zaakUuid) shouldBe "0"
            }
        }
    }

    given("a completed zaak whose CMMN case has ended, so that only its history is left") {
        val zaakUuid = createZaak(ZAAKTYPE_CMMN_TEST_2_UUID, RECORDMANAGER_1)
        doUserEventListenerPlanItem(
            zaakUuid,
            """
                "actie": "$ACTIE_INTAKE_AFRONDEN",
                "isZaakOntvankelijk": true
            """.trimIndent()
        )
        val resultaattypeUuid = itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_CMMN_TEST_2_UUID",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        doUserEventListenerPlanItem(
            zaakUuid,
            """
                "actie": "$ACTIE_ZAAK_AFHANDELEN",
                "resultaattypeUuid": "$resultaattypeUuid",
                "resultaatToelichting": "fakeResultaatToelichting"
            """.trimIndent()
        )
        countRows("act_cmmn_ru_case_inst", zaakUuid) shouldBe "0"
        countRows("act_cmmn_hi_case_inst", zaakUuid) shouldBe "1"

        `when`("the zaak destroy notification is received") {
            val response = sendZaakDestroyNotification(zaakUuid)

            then("the historic CMMN case instance of the zaak is deleted") {
                response.code shouldBe HTTP_NO_CONTENT
                countRows("act_cmmn_hi_case_inst", zaakUuid) shouldBe "0"
            }
        }
    }
})
