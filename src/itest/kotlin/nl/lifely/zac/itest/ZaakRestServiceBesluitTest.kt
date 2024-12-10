/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.lifely.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.util.sleep
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, then adds, updates, and withdraws a besluit to the zaak.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
@Suppress("MagicNumber")
class ZaakRestServiceBesluitTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak has been created that has finished the intake phase with the status 'admissible'") {
        lateinit var zaakUUID: UUID
        lateinit var resultaatType1Uuid: UUID
        lateinit var resultaatType2Uuid: UUID
        lateinit var besluitType1Uuid: UUID
        lateinit var besluitUuid: UUID
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            JSONObject(body!!.string()).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems"
        ).run {
            JSONArray(body!!.string()).getJSONObject(0).run {
                intakeId = getString("id").toInt()
            }
        }
        // Wait before setting the status of a zaak (implicitly)
        // because OpenZaak does not allow setting multiple statuses for one zaak
        // within the same timeframe of one second.
        // If we do not wait in these cases we get a 400 response from OpenZaak with:
        // "rest_framework.exceptions.ValidationError: {'non_field_errors':
        // [ErrorDetail(string='De velden zaak, datum_status_gezet moeten een unieke set zijn.', code='unique')]}"
        //
        // Related OpenZaak issue: https://github.com/open-zaak/open-zaak/issues/1639
        sleep(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
            {
                "zaakUuid":"$zaakUUID",
                "planItemInstanceId":"$intakeId",
                "actie":"$ACTIE_INTAKE_AFRONDEN",
                "zaakOntvankelijk":true
            }
            """.trimIndent()
        ).run {
            logger.info { "Response: ${body!!.string()}" }
            code shouldBe HTTP_STATUS_NO_CONTENT
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID"
        ).run {
            with(JSONArray(body!!.string())) {
                // we expect 4 resultaat types for this zaak type
                shouldHaveSize(4)
                resultaatType1Uuid = getJSONObject(0).getString("id").let(UUID::fromString)
                resultaatType2Uuid = getJSONObject(1).getString("id").let(UUID::fromString)
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/zaken/besluittypes/$ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID"
        ).run {
            with(JSONArray(body!!.string())) {
                // we expect 2 besluit types for this zaak type
                shouldHaveSize(2)
                besluitType1Uuid = getJSONObject(0).getString("id").let(UUID::fromString)
            }
        }

        When("a besluit is added to the zaak") {
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
                    "toelichting":"dummyToelichting",
                    "ingangsdatum":"$today",
                    "vervaldatum":"$tomorrow",
                    "publicationDate": "$publicationDate",
                    "lastResponseDate": "$responseDate"
                }
                """.trimIndent()
            ).run {
                logger.info { "Response: ${body!!.string()}" }
                code shouldBe HTTP_STATUS_OK
            }

            Then("the besluit has been created successfully") {
                itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID"
                ).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_STATUS_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    besluiten.getJSONObject(0).run {
                        getString("uuid") shouldNotBe null
                        getString("toelichting") shouldBe "dummyToelichting"
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

        When("the besluit is updated with a new result type, start date, end date, last response date and reason") {
            val startDate = LocalDate.now().plusDays(1)
            val fatalDate = LocalDate.now().plusDays(2)
            val newPublicationDate = LocalDate.now().plusMonths(3)
            val newResponseDate = LocalDate.now().plusMonths(4)
            val updateReason = "dummyBesluitUpdateToelichting"
            itestHttpClient.performPutRequest(
                "$ZAC_API_URI/zaken/besluit",
                requestBodyAsString = """
                {
                    "besluitUuid":"$besluitUuid",
                    "resultaattypeUuid":"$resultaatType2Uuid",
                    "toelichting":"$updateReason",
                    "ingangsdatum":"$startDate",
                    "vervaldatum":"$fatalDate",
                    "publicationDate":"$newPublicationDate",
                    "lastResponseDate": "$newResponseDate"         
                }
                """.trimIndent()
            ).use { response ->
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("isIngetrokken", false)
                    shouldContainJsonKeyValue("toelichting", updateReason)
                }
            }

            Then("the besluit should be successfully updated") {
                itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID"
                ).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_STATUS_OK
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

        When("a besluit is withdrawn from the zaak") {
            itestHttpClient.performPutRequest(
                "$ZAC_API_URI/zaken/besluit/intrekken",
                requestBodyAsString = """
            {
                "besluitUuid":"$besluitUuid",
                "reden":"dummyReason",
                "vervalreden":"ingetrokken_belanghebbende"
            }
                """.trimIndent()
            ).use { response ->
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("isIngetrokken", true)
                    shouldContainJsonKeyValue("toelichting", "dummyBesluitUpdateToelichting")
                    shouldContainJsonKeyValue("vervalreden", "ingetrokken_belanghebbende")
                }
            }

            Then("the besluit has been withdrawn successfully") {
                itestHttpClient.performGetRequest(
                    "$ZAC_API_URI/zaken/besluit/zaakUuid/$zaakUUID"
                ).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.code shouldBe HTTP_STATUS_OK
                    val besluiten = JSONArray(responseBody)
                    besluiten.shouldHaveSize(1)
                    with(besluiten.getJSONObject(0).toString()) {
                        shouldContainJsonKeyValue("isIngetrokken", true)
                        shouldContainJsonKeyValue("toelichting", "dummyBesluitUpdateToelichting")
                        shouldContainJsonKeyValue("vervalreden", "ingetrokken_belanghebbende")
                    }
                }
            }
        }
    }
})
