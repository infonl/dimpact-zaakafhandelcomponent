/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, then adds, updates,
 * and withdraws a besluit to the zaak.
 */
@Suppress("MagicNumber")
class ZaakBesluitRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    given(
        """
        A zaak has been created that has finished the intake phase with the status 'admissible',
        and a logged in behandelaar
        """
    ) {
        lateinit var zaakUUID: UUID
        lateinit var resultaatType1Uuid: UUID
        lateinit var resultaatType2Uuid: UUID
        lateinit var besluitType1Uuid: UUID
        lateinit var besluitUuid: UUID
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            JSONObject(bodyAsString).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems",
            testUser = BEHANDELAAR_1
        ).run {
            JSONArray(bodyAsString).getJSONObject(0).run {
                intakeId = getString("id").toInt()
            }
        }
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
                {
                    "zaakUuid":"$zaakUUID",
                    "planItemInstanceId":"$intakeId",
                    "actie":"$ACTIE_INTAKE_AFRONDEN",
                    "zaakOntvankelijk":true
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_NO_CONTENT
        }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_CMMN_TEST_2_UUID",
            testUser = BEHANDELAAR_1
        ).run {
            with(JSONArray(bodyAsString)) {
                // we expect 4 resultaat types for this zaak type
                shouldHaveSize(4)
                resultaatType1Uuid = getJSONObject(0).getString("id").let(UUID::fromString)
                resultaatType2Uuid = getJSONObject(1).getString("id").let(UUID::fromString)
            }
        }
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/besluittypes/$ZAAKTYPE_CMMN_TEST_2_UUID",
            testUser = BEHANDELAAR_1
        ).run {
            with(JSONArray(bodyAsString)) {
                // we expect 2 besluit types for this zaak type
                shouldHaveSize(2)
                besluitType1Uuid = getJSONObject(0).getString("id").let(UUID::fromString)
            }
        }

        `when`("a besluit is added to the zaak") {
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val publicationDate = today.plusMonths(2)
            val responseDate = today.plusMonths(3)

            itestHttpClient.performJSONPostRequest(
                "$ZAC_API_URI/zaken/besluit",
                requestBodyAsString = """
                    {
                        "zaakUuid":"$zaakUUID",
                        "resultaattypeUuid":"$resultaatType1Uuid",
                        "besluittypeUuid":"$besluitType1Uuid",
                        "toelichting":"fakeToelichting",
                        "ingangsdatum":"$today",
                        "vervaldatum":"$tomorrow",
                        "publicationDate": "$publicationDate",
                        "lastResponseDate": "$responseDate"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).run {
                logger.info { "Response: $bodyAsString" }
                code shouldBe HTTP_OK
            }

            then("the besluit has been created successfully") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID",
                    testUser = BEHANDELAAR_1
                ).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    besluiten.getJSONObject(0).run {
                        getString("uuid") shouldNotBe null
                        getString("toelichting") shouldBe "fakeToelichting"
                        getString("ingangsdatum") shouldBe today.toString()
                        getString("vervaldatum") shouldBe tomorrow.toString()
                        getString("publicationDate") shouldBe publicationDate.toString()
                        getString("lastResponseDate") shouldBe responseDate.toString()
                        getBoolean("isIngetrokken") shouldBe false
                        getJSONArray("informatieobjecten").shouldHaveSize(0)
                        getJSONObject("besluittype").run {
                            getString("id") shouldBe besluitType1Uuid.toString()
                            getString("naam") shouldBe "Besluit na heroverweging"
                            getString("toelichting") shouldBe "Besluit na heroverweging"
                        }
                    }
                    besluitUuid = besluiten.getJSONObject(0).getString("uuid").run(UUID::fromString)
                }
            }
        }

        `when`("the besluit is updated with a new result type, start date, end date, last response date and reason") {
            val startDate = LocalDate.now().plusDays(1)
            val fatalDate = LocalDate.now().plusDays(2)
            val newPublicationDate = LocalDate.now().plusMonths(3)
            val newResponseDate = LocalDate.now().plusMonths(4)
            val updateReason = "fakeBesluitUpdateToelichting"
            itestHttpClient.performPutRequest(
                "$ZAC_API_URI/zaken/besluit",
                requestBodyAsString = """
                    {
                        "besluitUuid":"$besluitUuid",
                        "reden":"fakeBesluitUpdateReason",
                        "resultaattypeUuid":"$resultaatType2Uuid",
                        "toelichting":"$updateReason",
                        "ingangsdatum":"$startDate",
                        "vervaldatum":"$fatalDate",
                        "publicationDate":"$newPublicationDate",
                        "lastResponseDate": "$newResponseDate"         
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).let { response ->
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("isIngetrokken", false)
                    shouldContainJsonKeyValue("toelichting", updateReason)
                }
            }

            then("the besluit should be successfully updated") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID",
                    testUser = BEHANDELAAR_1
                ).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    besluiten.getJSONObject(0).run {
                        getString("uuid") shouldNotBe null
                        getString("toelichting") shouldBe updateReason
                        getString("ingangsdatum") shouldBe startDate.toString()
                        getString("vervaldatum") shouldBe fatalDate.toString()
                        getString("publicationDate") shouldBe newPublicationDate.toString()
                        getString("lastResponseDate") shouldBe newResponseDate.toString()
                        getBoolean("isIngetrokken") shouldBe false
                        getJSONArray("informatieobjecten").shouldHaveSize(0)
                        getJSONObject("besluittype").run {
                            getString("id") shouldBe besluitType1Uuid.toString()
                            getString("naam") shouldBe "Besluit na heroverweging"
                            getString("toelichting") shouldBe "Besluit na heroverweging"
                        }
                    }
                }
            }
        }

        `when`("a besluit is withdrawn from the zaak") {
            itestHttpClient.performPutRequest(
                "$ZAC_API_URI/zaken/besluit/intrekken",
                requestBodyAsString = """
                    {
                        "besluitUuid":"$besluitUuid",
                        "reden":"fakeReason",
                        "vervalreden":"ingetrokken_belanghebbende"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).let { response ->
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("isIngetrokken", true)
                    shouldContainJsonKeyValue("toelichting", "fakeBesluitUpdateToelichting")
                    shouldContainJsonKeyValue("vervalreden", "ingetrokken_belanghebbende")
                }
            }

            then("the besluit has been withdrawn successfully") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID",
                    testUser = BEHANDELAAR_1
                ).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    with(besluiten.getJSONObject(0).toString()) {
                        shouldContainJsonKeyValue("isIngetrokken", true)
                        shouldContainJsonKeyValue("toelichting", "fakeBesluitUpdateToelichting")
                        shouldContainJsonKeyValue("vervalreden", "ingetrokken_belanghebbende")
                    }
                }
            }
        }

        `when`("the besluit history is requested after create, update, and withdrawal") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/besluit/$besluitUuid/historie",
                testUser = BEHANDELAAR_1
            )

            then(
                "the besluit history is returned with history items for the create, update, and withdrawal operations"
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(JSONArray(responseBody)) {
                    // 1 create line + 3 update lines (ingangsdatum, vervaldatum, toelichting);
                    // withdrawal only changes vervalreden/ingetrokken which are not tracked by AuditBesluitConverter
                    shouldHaveSize(4)
                    // history is sorted newest first; creation entry is last
                    getJSONObject(0).run {
                        getString("actie") shouldBe "GEWIJZIGD"
                        getString("attribuutLabel") shouldBe "ingangsdatum"
                        getString("door") shouldBe BEHANDELAAR_1.displayName
                        has("datumTijd") shouldBe true
                    }
                    getJSONObject(length() - 1).run {
                        getString("actie") shouldBe "GEKOPPELD"
                        getString("attribuutLabel") shouldBe "Besluit"
                        getString("door") shouldBe BEHANDELAAR_1.displayName
                        getString("nieuweWaarde") shouldNotBe null
                        has("datumTijd") shouldBe true
                    }
                }
            }
        }

        `when`("the besluit is updated with the optional vervaldatum cleared") {
            val ingangsdatum = LocalDate.now()
            val publicationDate = LocalDate.now().plusMonths(5)
            val responseDate = LocalDate.now().plusMonths(6)
            itestHttpClient.performPutRequest(
                "$ZAC_API_URI/zaken/besluit",
                requestBodyAsString = """
                    {
                        "besluitUuid":"$besluitUuid",
                        "reden":"fakeBesluitUpdateReason",
                        "toelichting":"fakeBesluitUpdateToelichting",
                        "ingangsdatum":"$ingangsdatum",
                        "publicationDate":"$publicationDate",
                        "lastResponseDate":"$responseDate"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).let { response ->
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
            }

            then("the cleared vervaldatum is persisted in Open Zaak when the besluit is retrieved") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID",
                    testUser = BEHANDELAAR_1
                ).let { response ->
                    val responseBody = response.bodyAsString
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    besluiten.getJSONObject(0).run {
                        // the vervaldatum has been cleared, so it is no longer present in the response
                        optString("vervaldatum", "") shouldBe ""
                        // isIngetrokken is derived from the vervaldatum, so clearing it un-marks the withdrawal
                        getBoolean("isIngetrokken") shouldBe false
                        // the other optional dates were supplied and remain set
                        getString("ingangsdatum") shouldBe ingangsdatum.toString()
                        getString("publicationDate") shouldBe publicationDate.toString()
                        getString("lastResponseDate") shouldBe responseDate.toString()
                    }
                }
            }
        }
    }
})
