/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val BRONDATUM_MONTHS_IN_FUTURE = 3L
private val BRONDATUM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'00:00:00.000XXX")

/**
 * This test creates a zaak, adds a task to complete the intake phase, closes the zaak with afleidingswijze 'eigenschap'.
 */
class ZaakRestServiceBrondatumAfleidingswijzeEigenschapArchiveTest : BehaviorSpec({
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
        zacClient.createZaakAndRetrieve(
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
                    .first { it.getString("naam") == "Opgelegd - Eigenschap" }
            }.run {
                resultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }

        `when`("the zaak is completed with afhandelwijze 'Opgelegd - Eigenschap' (afleidingswijze 'eigenschap'") {
            val afhandelenId: Int
            val brondatum = LocalDate.now().plusMonths(BRONDATUM_MONTHS_IN_FUTURE)
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
            val requestBodyAsString = """
                {
                    "zaakUuid":"$zaakUuid",
                    "planItemInstanceId":"$afhandelenId",
                    "actie":"$ACTIE_ZAAK_AFHANDELEN",
                    "resultaattypeUuid": "$resultaatTypeUuid",
                    "resultaatToelichting":"afronden",
                    "brondatum":"${
                        brondatum.atStartOfDay(ZoneId.systemDefault()).format(BRONDATUM_FORMATTER)
                    }"
                }
            """.trimIndent()
            logger.info { "Request body: $requestBodyAsString" }
            itestHttpClient.performJSONPostRequest(
                "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
                requestBodyAsString = requestBodyAsString,
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            then("the zaak should be closed and have a result and startdatumBewaartermijn = brondatum") {
                zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        LocalDate.parse(getString("startdatumBewaartermijn")) shouldBe brondatum
                    }
                }
            }
        }
    }

    given(
        """
        A second zaak has been created that has finished the intake phase with the status 'admissible'
        and a logged-in recordmanager for domain test 1
        """
    ) {
        lateinit var zaakUuid: UUID
        lateinit var resultaatTypeUuid: UUID
        val intakeId: Int
        zacClient.createZaakAndRetrieve(
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
                    .first { it.getString("naam") == "Opgelegd - Eigenschap" }
            }.run {
                resultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }

        `when`("the zaak is completed with afhandelwijze 'Opgelegd - Eigenschap' (afleidingswijze 'eigenschap' with an empty date") {
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
            val requestBodyAsString = """
                {
                    "zaakUuid":"$zaakUuid",
                    "planItemInstanceId":"$afhandelenId",
                    "actie":"$ACTIE_ZAAK_AFHANDELEN",
                    "resultaattypeUuid": "$resultaatTypeUuid",
                    "resultaatToelichting":"afronden",
                    "brondatum":null
                }
            """.trimIndent()
            logger.info { "Request body: $requestBodyAsString" }
            itestHttpClient.performJSONPostRequest(
                "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
                requestBodyAsString = requestBodyAsString,
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            then("the zaak should be closed and have a result and startdatumBewaartermijn = null") {
                zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                        shouldNotContainJsonKey("startdatumBewaartermijn")
                    }
                }
            }
        }

        `when`("the brondatum is set for the zaak") {
            val brondatum = LocalDate.now().plusMonths(BRONDATUM_MONTHS_IN_FUTURE)
            val requestBodyAsString = """
                {
                    "brondatum":"${
                brondatum.atStartOfDay(ZoneId.systemDefault()).format(BRONDATUM_FORMATTER)
            }"
                }
            """.trimIndent()
            logger.info { "Request body: $requestBodyAsString" }
            itestHttpClient.performPutRequest(
                "$ZAC_API_URI/zaken/zaak/${zaakUuid}/brondatum",
                requestBodyAsString = requestBodyAsString,
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            then("the zaak should be closed and have a result and startdatumBewaartermijn = brondatum") {
                zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        LocalDate.parse(getString("startdatumBewaartermijn")) shouldBe brondatum
                    }
                }
            }
        }
    }
})
