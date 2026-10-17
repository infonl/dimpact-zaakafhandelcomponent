/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_EN_BEHANDELAAR_1
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

private const val DEFAULT_ITEMS_PER_PAGE = 25
private const val UPDATED_ITEMS_PER_PAGE = 50
private const val UPDATED_CARD_COLUMN = 2
private const val UPDATED_CARD_ROW = 3

class GebruikersvoorkeurenRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val gebruikersvoorkeurenUri = "$ZAC_API_URI/gebruikersvoorkeuren"
    val testUser = RAADPLEGER_EN_BEHANDELAAR_1

    fun listZoekopdrachten() =
        itestHttpClient.performGetRequest(
            url = "$gebruikersvoorkeurenUri/zoekopdracht/MIJN_ZAKEN",
            testUser = testUser
        ).run {
            code shouldBe HTTP_OK
            JSONArray(bodyAsString).map { it as JSONObject }
        }

    fun createZoekopdracht(naam: String) =
        itestHttpClient.performJSONPostRequest(
            url = "$gebruikersvoorkeurenUri/zoekopdracht",
            requestBodyAsString = """
                {
                    "lijstID": "MIJN_ZAKEN",
                    "naam": "$naam",
                    "json": "{}"
                }
            """.trimIndent(),
            testUser = testUser
        )

    fun listDashboardCards() =
        itestHttpClient.performGetRequest(
            url = "$gebruikersvoorkeurenUri/dasboardcard/actief",
            testUser = testUser
        ).run {
            code shouldBe HTTP_OK
            JSONArray(bodyAsString).map { it as JSONObject }
        }

    fun isTaakOpNaamShownOnDashboard() =
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/signaleringen/instellingen",
            testUser = testUser
        ).run {
            code shouldBe HTTP_OK
            JSONArray(bodyAsString)
                .map { it as JSONObject }
                .first { it.getString("type") == "TAAK_OP_NAAM" }
                .getBoolean("isDashboardEnabled")
        }

    context("Tabel instellingen") {
        given("a medewerker without tabel instellingen for the werklijst 'mijn zaken'") {
            `when`("the tabel gegevens are read") {
                val response = itestHttpClient.performGetRequest(
                    url = "$gebruikersvoorkeurenUri/tabel-gegevens/MIJN_ZAKEN",
                    testUser = testUser
                )

                then("the default number of items per page is returned") {
                    response.code shouldBe HTTP_OK
                    JSONObject(response.bodyAsString).getInt("aantalPerPagina") shouldBe DEFAULT_ITEMS_PER_PAGE
                }
            }

            `when`("the number of items per page is set to 50 and the tabel gegevens are read again") {
                val updateResponse = itestHttpClient.performPutRequest(
                    url = "$gebruikersvoorkeurenUri/aantal-per-pagina/MIJN_ZAKEN/$UPDATED_ITEMS_PER_PAGE",
                    requestBodyAsString = "",
                    testUser = testUser
                )
                val readResponse = itestHttpClient.performGetRequest(
                    url = "$gebruikersvoorkeurenUri/tabel-gegevens/MIJN_ZAKEN",
                    testUser = testUser
                )

                then("the new number of items per page is returned") {
                    updateResponse.code shouldBe HTTP_NO_CONTENT
                    readResponse.code shouldBe HTTP_OK
                    JSONObject(readResponse.bodyAsString).getInt("aantalPerPagina") shouldBe UPDATED_ITEMS_PER_PAGE
                }
            }
        }
    }

    context("Zoekopdrachten") {
        given("a medewerker without zoekopdrachten for the werklijst 'mijn zaken'") {
            var firstZoekopdrachtId = 0L
            var secondZoekopdrachtId = 0L

            `when`("a zoekopdracht is created") {
                val response = createZoekopdracht("fakeZoekopdracht1")

                then("the zoekopdracht is returned and is the active one") {
                    response.code shouldBe HTTP_OK
                    with(JSONObject(response.bodyAsString)) {
                        firstZoekopdrachtId = getLong("id")
                        getString("naam") shouldBe "fakeZoekopdracht1"
                        getBoolean("actief") shouldBe true
                    }
                }
            }

            `when`("a zoekopdracht is created with the same name in other letter case") {
                val response = createZoekopdracht("FAKEZOEKOPDRACHT1")

                then("the zoekopdracht is not created") {
                    response.code shouldBe HTTP_INTERNAL_ERROR
                    listZoekopdrachten().size shouldBe 1
                }
            }

            `when`("a second zoekopdracht is created") {
                val response = createZoekopdracht("fakeZoekopdracht2")

                then("the second zoekopdracht becomes the active one") {
                    response.code shouldBe HTTP_OK
                    secondZoekopdrachtId = JSONObject(response.bodyAsString).getLong("id")
                    listZoekopdrachten().associate { it.getLong("id") to it.getBoolean("actief") } shouldBe mapOf(
                        firstZoekopdrachtId to false,
                        secondZoekopdrachtId to true
                    )
                }
            }

            `when`("the first zoekopdracht is made active") {
                val response = itestHttpClient.performPutRequest(
                    url = "$gebruikersvoorkeurenUri/zoekopdracht/actief",
                    requestBodyAsString = """
                        {
                            "id": $firstZoekopdrachtId,
                            "lijstID": "MIJN_ZAKEN",
                            "naam": "fakeZoekopdracht1",
                            "json": "{}"
                        }
                    """.trimIndent(),
                    testUser = testUser
                )

                then("only the first zoekopdracht is active") {
                    response.code shouldBe HTTP_NO_CONTENT
                    listZoekopdrachten().associate { it.getLong("id") to it.getBoolean("actief") } shouldBe mapOf(
                        firstZoekopdrachtId to true,
                        secondZoekopdrachtId to false
                    )
                }
            }

            `when`("the active zoekopdracht of the werklijst is removed") {
                val response = itestHttpClient.performDeleteRequest(
                    url = "$gebruikersvoorkeurenUri/zoekopdracht/MIJN_ZAKEN/actief",
                    testUser = testUser
                )

                then("no zoekopdracht is active") {
                    response.code shouldBe HTTP_NO_CONTENT
                    listZoekopdrachten().map { it.getBoolean("actief") } shouldBe listOf(false, false)
                }
            }

            `when`("both zoekopdrachten are deleted") {
                val firstResponse = itestHttpClient.performDeleteRequest(
                    url = "$gebruikersvoorkeurenUri/zoekopdracht/$firstZoekopdrachtId",
                    testUser = testUser
                )
                val secondResponse = itestHttpClient.performDeleteRequest(
                    url = "$gebruikersvoorkeurenUri/zoekopdracht/$secondZoekopdrachtId",
                    testUser = testUser
                )

                then("the medewerker has no zoekopdrachten left") {
                    firstResponse.code shouldBe HTTP_NO_CONTENT
                    secondResponse.code shouldBe HTTP_NO_CONTENT
                    listZoekopdrachten().size shouldBe 0
                }
            }
        }
    }

    context("Dashboard cards") {
        given("a medewerker without dashboard cards") {
            var taakOpNaamCardId = 0L

            `when`("a dashboard card without signalering type is added") {
                val response = itestHttpClient.performPutRequest(
                    url = "$gebruikersvoorkeurenUri/dasboardcard",
                    requestBodyAsString = """{ "cardId": "MIJN_ZAKEN", "column": 0, "row": 0 }""",
                    testUser = testUser
                )

                then("the dashboard card is returned") {
                    response.code shouldBe HTTP_OK
                    JSONArray(response.bodyAsString).map { (it as JSONObject).getString("cardId") } shouldBe
                        listOf("MIJN_ZAKEN")
                }
            }

            `when`("a dashboard card for the signalering type 'taak op naam' is added") {
                val response = itestHttpClient.performPutRequest(
                    url = "$gebruikersvoorkeurenUri/dasboardcard",
                    requestBodyAsString = """
                        { "cardId": "MIJN_TAKEN_NIEUW", "signaleringType": "TAAK_OP_NAAM", "column": 1, "row": 0 }
                    """.trimIndent(),
                    testUser = testUser
                )

                then("both dashboard cards are returned and the signalering is shown on the dashboard") {
                    response.code shouldBe HTTP_OK
                    val cards = JSONArray(response.bodyAsString).map { it as JSONObject }
                    cards.map { it.getString("cardId") }.toSet() shouldBe setOf("MIJN_ZAKEN", "MIJN_TAKEN_NIEUW")
                    taakOpNaamCardId = cards.first { it.getString("cardId") == "MIJN_TAKEN_NIEUW" }.getLong("id")
                    isTaakOpNaamShownOnDashboard() shouldBe true
                }
            }

            `when`("the dashboard cards are updated to only the 'taak op naam' card in another position") {
                val response = itestHttpClient.performPutRequest(
                    url = "$gebruikersvoorkeurenUri/dasboardcard/actief",
                    requestBodyAsString = """
                        [{ "id": $taakOpNaamCardId, "cardId": "MIJN_TAKEN_NIEUW", "column": $UPDATED_CARD_COLUMN, "row": $UPDATED_CARD_ROW }]
                    """.trimIndent(),
                    testUser = testUser
                )

                then("the other dashboard card is removed and the 'taak op naam' card has the new position") {
                    response.code shouldBe HTTP_OK
                    with(JSONArray(response.bodyAsString).map { it as JSONObject }.single()) {
                        getLong("id") shouldBe taakOpNaamCardId
                        getInt("column") shouldBe UPDATED_CARD_COLUMN
                        getInt("row") shouldBe UPDATED_CARD_ROW
                    }
                }
            }

            `when`("the 'taak op naam' dashboard card is deleted") {
                val response = itestHttpClient.performDeleteRequest(
                    url = "$gebruikersvoorkeurenUri/dasboardcard",
                    requestBodyAsString = """
                        { "id": $taakOpNaamCardId, "cardId": "MIJN_TAKEN_NIEUW", "signaleringType": "TAAK_OP_NAAM" }
                    """.trimIndent(),
                    testUser = testUser
                )

                then("no dashboard cards are left and the signalering is no longer shown on the dashboard") {
                    response.code shouldBe HTTP_OK
                    JSONArray(response.bodyAsString).length() shouldBe 0
                    listDashboardCards().size shouldBe 0
                    isTaakOpNaamShownOnDashboard() shouldBe false
                }
            }
        }
    }
})
