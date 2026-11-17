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
import java.net.HttpURLConnection.HTTP_OK

private const val ALL_ROWS = 1000

class ZaaktypeConfigurationRestServicePagingTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    fun listZaaktypeConfigurationPage(requestBody: String) = itestHttpClient.performPutRequest(
        url = "$ZAC_API_URI/zaakafhandelparameters",
        requestBodyAsString = requestBody,
        testUser = BEHEERDER_1
    ).also { logger.info { "Response: ${it.bodyAsString}" } }

    fun beginGeldigheidRangeBody(van: String?, tot: String?) = JSONObject()
        .put("page", 0)
        .put("rows", ALL_ROWS)
        .put("beginGeldigheid", JSONObject().put("van", van ?: JSONObject.NULL).put("tot", tot ?: JSONObject.NULL))
        .toString()

    fun ResponseContent.zaaktypeConfigurationOverzichten() =
        JSONObject(bodyAsString).getJSONArray("resultaten").map { it as JSONObject }

    given("Zaaktype CMMN configurations have been created for the CMMN test zaaktypes, and a beheerder is logged in") {
        `when`("the beheerder lists the first page of one row of the zaaktype configurations, sorted on omschrijving") {
            val response = listZaaktypeConfigurationPage(
                """
                {
                  "page": 0,
                  "rows": 1,
                  "sortField": "OMSCHRIJVING",
                  "sortOrder": "ASC"
                }
                """.trimIndent()
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

        `when`("the beheerder lists the valid zaaktype configurations of the '$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION' zaaktype") {
            val response = listZaaktypeConfigurationPage(
                """
                {
                  "page": 0,
                  "rows": 25,
                  "zaaktypeIdentificatie": "$ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE",
                  "isValide": true
                }
                """.trimIndent()
            )
            val restZaaktypeConfigurationOverzichtResultaat = JSONObject(response.bodyAsString)
            val restZaaktypeConfigurationOverzichten = restZaaktypeConfigurationOverzichtResultaat
                .getJSONArray("resultaten")
                .map { it as JSONObject }

            then("only the valid configurations of the versions of that zaaktype are returned") {
                response.code shouldBe HTTP_OK
                restZaaktypeConfigurationOverzichten.map { it.getJSONObject("zaaktype").getString("uuid") }
                    .shouldContain(ZAAKTYPE_CMMN_TEST_1_UUID.toString())
                restZaaktypeConfigurationOverzichten.forEach {
                    it.getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_CMMN_TEST_1_IDENTIFICATIE
                    it.getBoolean("isValide") shouldBe true
                }
                restZaaktypeConfigurationOverzichtResultaat.getLong("totaal") shouldBe
                    restZaaktypeConfigurationOverzichten.size.toLong()
            }
        }

        `when`(
            "the beheerder lists the zaaktype configurations whose begin geldigheid lies in a range that starts and " +
                "ends on the begin geldigheid of the '$ZAAKTYPE_CMMN_TEST_1_DESCRIPTION' zaaktype"
        ) {
            val response = listZaaktypeConfigurationPage(
                beginGeldigheidRangeBody(ZAAKTYPE_CMMN_TEST_1_BEGIN_GELDIGHEID, ZAAKTYPE_CMMN_TEST_1_BEGIN_GELDIGHEID)
            )
            val restZaaktypeConfigurationOverzichten = response.zaaktypeConfigurationOverzichten()

            then("the configuration of that zaaktype is returned, so both ends of the range are included") {
                response.code shouldBe HTTP_OK
                restZaaktypeConfigurationOverzichten.map { it.getJSONObject("zaaktype").getString("uuid") }
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
                beginGeldigheidRangeBody(van = "2025-07-02", tot = null)
            ).zaaktypeConfigurationOverzichten()
            val rangeUntilTheDayBefore = listZaaktypeConfigurationPage(
                beginGeldigheidRangeBody(van = null, tot = "2025-06-30")
            ).zaaktypeConfigurationOverzichten()

            then("the configuration of that zaaktype is not returned") {
                listOf(rangeFromTheDayAfter, rangeUntilTheDayBefore).forEach { restZaaktypeConfigurationOverzichten ->
                    restZaaktypeConfigurationOverzichten.map { it.getJSONObject("zaaktype").getString("uuid") }
                        .shouldNotContain(ZAAKTYPE_CMMN_TEST_1_UUID.toString())
                }
            }
        }
    }
})
