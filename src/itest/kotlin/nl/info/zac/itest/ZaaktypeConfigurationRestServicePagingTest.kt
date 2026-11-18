/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ResponseContent
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_BEGIN_GELDIGHEID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

private const val ALL_ROWS = 1000

class ZaaktypeConfigurationRestServicePagingTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    fun listZaaktypeConfigurationPage(vararg queryParameters: Pair<String, Any>) = itestHttpClient.performGetRequest(
        url = "$ZAC_API_URI/zaakafhandelparameters?" + queryParameters.joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value.toString(), UTF_8)}"
        },
        testUser = BEHEERDER_1
    ).also { logger.info { "Response: ${it.bodyAsString}" } }

    fun ResponseContent.zaaktypeConfigurationOverzichten() =
        JSONObject(bodyAsString).getJSONArray("resultaten").map { it as JSONObject }

    fun List<JSONObject>.zaaktypeUuids() = map { it.getJSONObject("zaaktype").getString("uuid") }

    given("Zaaktype CMMN configurations have been created for the CMMN test zaaktypes, and a beheerder is logged in") {
        `when`("the beheerder lists the first page of one row of the zaaktype configurations, sorted on omschrijving") {
            val response = listZaaktypeConfigurationPage(
                "page" to 0,
                "rows" to 1,
                "sortField" to "OMSCHRIJVING",
                "sortOrder" to "ASC"
            )
            val restZaaktypeConfigurationOverzichtResultaat = JSONObject(response.bodyAsString)

            then("the response is ok, with one row and the number of all zaaktype configurations") {
                response.code shouldBe HTTP_OK
                restZaaktypeConfigurationOverzichtResultaat.getJSONArray("resultaten").length() shouldBe 1
                restZaaktypeConfigurationOverzichtResultaat.getLong("totaal") shouldBeGreaterThan 1
            }
            and("the zaaktypes and process definitions of all zaaktype configurations can be filtered on") {
                restZaaktypeConfigurationOverzichtResultaat.getJSONArray("filterZaaktypes")
                    .map { (it as JSONObject).getString("identificatie") }
                    .shouldContainAll(ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE, ZAAKTYPE_CMMN_TEST_2_IDENTIFICATIE)
                restZaaktypeConfigurationOverzichtResultaat.getJSONArray("filterProcessDefinitions")
                    .map { (it as JSONObject).getString("key") }
                    .shouldContain("generiek-zaakafhandelmodel")
            }
        }

        `when`("the beheerder lists the zaaktype configurations without query parameters") {
            val response = listZaaktypeConfigurationPage()
            val firstPageOf25Rows = listZaaktypeConfigurationPage("page" to 0, "rows" to 25)

            then("the same rows are returned as for the first page of 25 rows") {
                response.code shouldBe HTTP_OK
                response.zaaktypeConfigurationOverzichten().zaaktypeUuids() shouldBe
                    firstPageOf25Rows.zaaktypeConfigurationOverzichten().zaaktypeUuids()
            }
        }

        `when`("the beheerder lists the valid zaaktype configurations of the '$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION' zaaktype") {
            val response = listZaaktypeConfigurationPage(
                "rows" to ALL_ROWS,
                "zaaktypeIdentificatie" to ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE,
                "isValide" to true
            )
            val restZaaktypeConfigurationOverzichten = response.zaaktypeConfigurationOverzichten()

            then("only the valid configurations of the versions of that zaaktype are returned") {
                response.code shouldBe HTTP_OK
                restZaaktypeConfigurationOverzichten.zaaktypeUuids()
                    .shouldContain(ZAAKTYPE_CMMN_TEST_1_UUID.toString())
                restZaaktypeConfigurationOverzichten.forEach {
                    it.getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE
                    it.getBoolean("isValide") shouldBe true
                }
                JSONObject(response.bodyAsString).getLong("totaal") shouldBe
                    restZaaktypeConfigurationOverzichten.size.toLong()
            }
        }

        `when`(
            "the beheerder lists the zaaktype configurations whose begin geldigheid lies in a range that starts and " +
                "ends on the begin geldigheid of the '$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION' zaaktype"
        ) {
            val response = listZaaktypeConfigurationPage(
                "rows" to ALL_ROWS,
                "beginGeldigheidFrom" to ZAAKTYPE_CMMN_TEST_1_BEGIN_GELDIGHEID,
                "beginGeldigheidThrough" to ZAAKTYPE_CMMN_TEST_1_BEGIN_GELDIGHEID
            )
            val restZaaktypeConfigurationOverzichten = response.zaaktypeConfigurationOverzichten()

            then("the configuration of that zaaktype is returned, so both ends of the range are included") {
                response.code shouldBe HTTP_OK
                restZaaktypeConfigurationOverzichten.zaaktypeUuids()
                    .shouldContain(ZAAKTYPE_CMMN_TEST_1_UUID.toString())
            }
            and("every returned configuration belongs to a zaaktype with that begin geldigheid") {
                restZaaktypeConfigurationOverzichten.forEach {
                    it.getJSONObject("zaaktype").getString("beginGeldigheid") shouldBe
                        ZAAKTYPE_CMMN_TEST_1_BEGIN_GELDIGHEID
                }
            }
        }

        `when`(
            "the beheerder lists the zaaktype configurations whose begin geldigheid lies in a range that starts the " +
                "day after, or ends the day before, the begin geldigheid of the '$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION' " +
                "zaaktype"
        ) {
            val rangeFromTheDayAfter = listZaaktypeConfigurationPage(
                "rows" to ALL_ROWS,
                "beginGeldigheidFrom" to "2025-07-02"
            ).zaaktypeConfigurationOverzichten()
            val rangeUntilTheDayBefore = listZaaktypeConfigurationPage(
                "rows" to ALL_ROWS,
                "beginGeldigheidThrough" to "2025-06-30"
            ).zaaktypeConfigurationOverzichten()

            then("the configuration of that zaaktype is not returned") {
                listOf(rangeFromTheDayAfter, rangeUntilTheDayBefore).forEach {
                    it.zaaktypeUuids().shouldNotContain(ZAAKTYPE_CMMN_TEST_1_UUID.toString())
                }
            }
        }

        `when`("the beheerder lists the zaaktype configurations with a sort order that does not exist") {
            val response = listZaaktypeConfigurationPage("sortField" to "OMSCHRIJVING", "sortOrder" to "FOO")

            then("the request is refused with the status that JAX-RS gives for a query parameter it cannot read") {
                response.code shouldBe HTTP_NOT_FOUND
            }
        }

        `when`("the beheerder lists the zaaktype configurations with an empty sort order") {
            val emptySortOrder = listZaaktypeConfigurationPage(
                "rows" to ALL_ROWS,
                "sortField" to "OMSCHRIJVING",
                "sortOrder" to ""
            )
            val ascendingSortOrder = listZaaktypeConfigurationPage(
                "rows" to ALL_ROWS,
                "sortField" to "OMSCHRIJVING",
                "sortOrder" to "ASC"
            )

            then("the configurations are sorted ascending, as without a sort order") {
                emptySortOrder.code shouldBe HTTP_OK
                emptySortOrder.zaaktypeConfigurationOverzichten().zaaktypeUuids() shouldBe
                    ascendingSortOrder.zaaktypeConfigurationOverzichten().zaaktypeUuids()
            }
        }

        `when`("the beheerder lists the zaaktype configurations with a begin geldigheid that has a time in UTC") {
            val response = listZaaktypeConfigurationPage("beginGeldigheidFrom" to "2025-06-30T22:00:00.000Z")

            then("the request is refused, so the time is never read as another day") {
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }
    }
})
