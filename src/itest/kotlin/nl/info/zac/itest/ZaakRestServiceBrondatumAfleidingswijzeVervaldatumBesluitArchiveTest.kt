/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.time.LocalDate
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, closes the zaak with afleidingswijze 'vervaldatum_besluit'.
 */
@Suppress("MagicNumber")
class ZaakRestServiceBrondatumAfleidingswijzeVervaldatumBesluitArchiveTest : BehaviorSpec({
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
        lateinit var besluitTypeUuid: UUID
        lateinit var vervaldatumBesluit: LocalDate
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = ItestConfiguration.DATE_TIME_2000_01_01,
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
            url = "${ItestConfiguration.ZAC_API_URI}/planitems/zaak/$zaakUuid/userEventListenerPlanItems",
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
            "${ItestConfiguration.ZAC_API_URI}/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
                {
                    "zaakUuid":"$zaakUuid",
                    "planItemInstanceId":"$intakeId",
                    "actie":"${ItestConfiguration.ACTIE_INTAKE_AFRONDEN}",
                    "zaakOntvankelijk":true
                }
            """.trimIndent(),
            testUser = RECORDMANAGER_1
        ).run {
            code shouldBe HttpURLConnection.HTTP_NO_CONTENT
        }
        itestHttpClient.performGetRequest(
            url = "${ItestConfiguration.ZAC_API_URI}/zaken/resultaattypes/${ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID}",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).let { resultaattypes ->
                (0 until resultaattypes.length())
                    .map(resultaattypes::getJSONObject)
                    .first { it.getString("naam") == "Opgelegd - Verval besluit" }
            }.run {
                resultaatTypeUuid = getString("id").let(UUID::fromString)
            }
        }

        itestHttpClient.performGetRequest(
            url = "${ItestConfiguration.ZAC_API_URI}/zaken/besluittypes/${ItestConfiguration.ZAAKTYPE_CMMN_TEST_4_UUID}",
            testUser = BEHANDELAAR_1
        ).run {
            with(JSONArray(bodyAsString)) {
                // we expect one besluit type for this zaak type
                shouldHaveSize(1)
                besluitTypeUuid = getJSONObject(0).getString("id").let(UUID::fromString)
            }
        }

        `when`("a besluit is added to the zaak") {
            val ingangsdatumBesluit = LocalDate.now().plusDays(3)
            vervaldatumBesluit = LocalDate.now().plusDays(21)
            val today = LocalDate.now()
            val publicationDate = today.plusMonths(2)
            val responseDate = today.plusMonths(3)

            logger.info { "Vervaldatum besluit $vervaldatumBesluit" }

            itestHttpClient.performJSONPostRequest(
                "${ItestConfiguration.ZAC_API_URI}/zaken/besluit",
                requestBodyAsString = """
                    {
                        "zaakUuid":"$zaakUuid",
                        "resultaattypeUuid":"$resultaatTypeUuid",
                        "besluittypeUuid":"$besluitTypeUuid",
                        "toelichting":"fakeToelichting",
                        "ingangsdatum":"$ingangsdatumBesluit",
                        "vervaldatum":"$vervaldatumBesluit",
                        "publicationDate": "$publicationDate",
                        "lastResponseDate": "$responseDate"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).run {
                logger.info { "Response: $bodyAsString" }
                code shouldBe HttpURLConnection.HTTP_OK
            }

            then("the besluit has been created successfully") {
                itestHttpClient.performGetRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/zaken/besluit/zaakUuid/$zaakUuid",
                    testUser = BEHANDELAAR_1
                ).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpURLConnection.HTTP_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    besluiten.getJSONObject(0).run {
                        getString("uuid") shouldNotBe null
                        getString("toelichting") shouldBe "fakeToelichting"
                        getString("ingangsdatum") shouldBe ingangsdatumBesluit.toString()
                        getString("vervaldatum") shouldBe vervaldatumBesluit.toString()
                        getString("publicationDate") shouldBe publicationDate.toString()
                        getString("lastResponseDate") shouldBe responseDate.toString()
                        getBoolean("isIngetrokken") shouldBe false
                        getJSONArray("informatieobjecten").shouldHaveSize(0)
                        getJSONObject("besluittype").run {
                            getString("id") shouldBe besluitTypeUuid.toString()
                            getString("naam") shouldBe "Besluit na heroverweging"
                            getString("toelichting") shouldBe "Besluit na heroverweging"
                        }
                    }
                }
            }
        }

        `when`("the zaak is completed with afhandelwijze 'Opgelegd - Verval besluit' (afleidingswijze 'vervaldatum_besluit'") {
            val afhandelenId: Int
            itestHttpClient.performGetRequest(
                url = "${ItestConfiguration.ZAC_API_URI}/planitems/zaak/$zaakUuid/userEventListenerPlanItems",
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
                "${ItestConfiguration.ZAC_API_URI}/planitems/doUserEventListenerPlanItem",
                requestBodyAsString = """
                    {
                        "zaakUuid":"$zaakUuid",
                        "planItemInstanceId":"$afhandelenId",
                        "actie":"${ItestConfiguration.ACTIE_ZAAK_AFHANDELEN}",
                        "resultaattypeUuid": "$resultaatTypeUuid",
                        "resultaatToelichting":"afronden"
                    }
                """.trimIndent(),
                testUser = RECORDMANAGER_1
            ).run {
                code shouldBe HttpURLConnection.HTTP_NO_CONTENT
            }

            then("the zaak should be closed and have a result and startdatumBewaartermijn = vervaldatum besluit") {
                zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HttpURLConnection.HTTP_OK
                    responseBody.run {
                        shouldContainJsonKeyValue("isOpen", false)
                        shouldContainJsonKey("resultaat")
                    }
                    JSONObject(responseBody).run {
                        LocalDate.parse(getString("startdatumBewaartermijn")) shouldBe vervaldatumBesluit
                    }
                }
            }
        }
    }
})
