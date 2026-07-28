/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_ZAAK_AFHANDELEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, closes the zaak with afleidingswijze 'termijn'.
 */
class ZaakRestServiceBrondatumAfleidingswijzeTermijnArchiveTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    given(
        """
        A zaak has been created that has finished the intake phase with the status 'admissible'
        and a logged-in recordmanager for domain test 1
        """
    ) {
        lateinit var zaakUuid: UUID
        lateinit var resultaatTypeUuid: UUID
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_4_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    zaakUuid = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$zaakUuid/userEventListenerPlanItems",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).run {
                intakeId = getString("id").toInt()
            }
        }
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
                {
                    "zaakUuid":"$zaakUuid",
                    "planItemInstanceId":"$intakeId",
                    "actie":"$ACTIE_INTAKE_AFRONDEN",
                    "zaakOntvankelijk":true
                }
            """.trimIndent(),
            testUser = RECORDMANAGER_1
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_CMMN_TEST_4_UUID",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).let { resultaattypes ->
                (0 until resultaattypes.length())
                    .map(resultaattypes::getJSONObject)
                    .first { it.getString("naam") == "Opgelegd - Termijn" }
            }.run {
                resultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }

        `when`("the zaak is completed with afhandelwijze 'Opgelegd - Termijn' (afleidingswijze 'termijn'") {
            val afhandelenId: Int
            itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/planitems/zaak/$zaakUuid/userEventListenerPlanItems",
                testUser = RECORDMANAGER_1
            ).run {
                val responseBody = bodyAsString
                logger.info { "Response: $responseBody" }
                JSONArray(responseBody).getJSONObject(0).run {
                    afhandelenId = getString("id").toInt()
                }
            }
            // wait for OpenZaak to accept this request
            sleepForOpenZaakUniqueConstraint(1)
            itestHttpClient.performJSONPostRequest(
                "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
                requestBodyAsString = """
                    {
                        "zaakUuid":"$zaakUuid",
                        "planItemInstanceId":"$afhandelenId",
                        "actie":"$ACTIE_ZAAK_AFHANDELEN",
                        "resultaattypeUuid": "$resultaatTypeUuid",
                        "resultaatToelichting":"afronden"
                    }
                """.trimIndent(),
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            then("the zaak should be closed and have a result and startdatumBewaartermijn = einddatum + 1 year") {
                zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        LocalDate.parse(getString("startdatumBewaartermijn")) shouldBe
                            LocalDate.parse(getString("einddatum")).plusYears(1)
                    }
                }
            }
        }
    }
})
