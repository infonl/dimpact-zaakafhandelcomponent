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
import io.kotest.matchers.shouldNotBe
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
import java.util.UUID

/**
 * This test creates both a zaak and a deelzaak, adds a task to complete the intake phase for both,
 * links both zaken, closes the zaak with afleidingswijze 'hoofdzaak' and finally closes the zaak
 * with afleidingswijze 'afgehandeld'.
 */
class ZaakRestServiceBrondatumAfleidingswijzeHoofdzaakArchiveTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    given(
        """
        A hoofdzaak and deelzaak have been created that has finished the intake phase with the status 'admissible'
        and a logged-in recordmanager for domain test 1
        """
    ) {
        lateinit var hoofdzaakUuid: UUID
        lateinit var deelzaakUuid: UUID
        lateinit var hoofdzaakResultaatTypeUuid: UUID
        lateinit var deelzaakResultaatTypeUuid: UUID
        lateinit var einddatumHoofdzaak: String
        val intakeId: Int
        val deelzaakIntakeId: Int
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_4_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = RECORDMANAGER_1,
            description = "Hoofdzaak"
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    hoofdzaakUuid = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        logger.info { "zaakUuid: $hoofdzaakUuid" }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$hoofdzaakUuid/userEventListenerPlanItems",
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
                    "zaakUuid":"$hoofdzaakUuid",
                    "planItemInstanceId":"$intakeId",
                    "actie":"$ACTIE_INTAKE_AFRONDEN",
                    "zaakOntvankelijk":true
                }
            """.trimIndent(),
            testUser = RECORDMANAGER_1
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_4_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = RECORDMANAGER_1,
            description = "Deelzaak"
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONObject(responseBody).run {
                getJSONObject("zaakdata").run {
                    deelzaakUuid = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        logger.info { "deelzaakUuid: $deelzaakUuid" }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$deelzaakUuid/userEventListenerPlanItems",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).run {
                deelzaakIntakeId = getString("id").toInt()
            }
        }
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
                {
                    "zaakUuid":"$deelzaakUuid",
                    "planItemInstanceId":"$deelzaakIntakeId",
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
                    .first { it.getString("naam") == "Niet opgelegd" }
            }.run {
                hoofdzaakResultaatTypeUuid = getString("id").let(UUID::fromString)
            }
            JSONArray(responseBody).let { resultaattypes ->
                (0 until resultaattypes.length())
                    .map(resultaattypes::getJSONObject)
                    .first { it.getString("naam") == "Opgelegd - Hoofdzaak" }
            }.run {
                deelzaakResultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/koppel",
            requestBodyAsString = """
                    {
                        "zaakUuid": "$hoofdzaakUuid",
                        "teKoppelenZaakUuid": "$deelzaakUuid",
                        "relatieType": "DEELZAAK"
                    }
                """.trimIndent(),
            testUser = RECORDMANAGER_1
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }

        `when`("the deelzaak is completed with afhandelwijze 'Opgelegd - Hoofdzaak' (afleidingswijze 'hoofdzaak')") {
            val afhandelenId: Int
            itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/planitems/zaak/$deelzaakUuid/userEventListenerPlanItems",
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
                        "zaakUuid":"$deelzaakUuid",
                        "planItemInstanceId":"$afhandelenId",
                        "actie":"$ACTIE_ZAAK_AFHANDELEN",
                        "resultaattypeUuid": "$deelzaakResultaatTypeUuid",
                        "resultaatToelichting":"afronden"
                    }
                """.trimIndent(),
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            then("the deelzaak should be closed and have a result and einddatum should not be empty") {
                zacClient.retrieveZaak(deelzaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        getString("einddatum") shouldNotBe ""
                    }
                }
            }
        }

        `when`("the hoofdzaak is completed with afhandelwijze 'Niet opgelegd' (afleidingswijze 'afgehandeld')") {
            val afhandelenId: Int
            itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/planitems/zaak/$hoofdzaakUuid/userEventListenerPlanItems",
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
                        "zaakUuid":"$hoofdzaakUuid",
                        "planItemInstanceId":"$afhandelenId",
                        "actie":"$ACTIE_ZAAK_AFHANDELEN",
                        "resultaattypeUuid": "$hoofdzaakResultaatTypeUuid",
                        "resultaatToelichting":"afronden"
                    }
                """.trimIndent(),
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HTTP_NO_CONTENT
            }

            then("the hoofdzaak should be closed and have a result and startdatumBewaartermijn = einddatum") {
                zacClient.retrieveZaak(hoofdzaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        einddatumHoofdzaak = getString("einddatum")
                        getString("startdatumBewaartermijn") shouldBe einddatumHoofdzaak
                    }
                }
            }

            then("the deelzaak should be closed and have a result and startdatumBewaartermijn = einddatum hoofdzaak") {
                zacClient.retrieveZaak(deelzaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        getString("startdatumBewaartermijn") shouldBe einddatumHoofdzaak
                    }
                }
            }
        }
    }
})
