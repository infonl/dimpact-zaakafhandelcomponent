/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject

@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class NoteServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("An existing zaak") {
        When("the 'create note' endpoint is called") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notities",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakProductaanvraag1Uuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "$TEST_USER_1_USERNAME"
                    }
                """.trimIndent()
            )
            Then(
                "the created note should be returned"
            ) {
                response.isSuccessful shouldBe true
                val responseBody = response.body!!.string()
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                        "zaakUUID": "$zaakProductaanvraag1Uuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "$TEST_USER_1_USERNAME",
                        "voornaamAchternaamMedewerker": "$TEST_USER_1_NAME",
                        "bewerkenToegestaan": true
                    }
                """.trimIndent()
                with(JSONObject(responseBody)) {
                    get("id") shouldNotBe null
                    get("tijdstipLaatsteWijziging") shouldNotBe null
                }
            }
        }
        When("the 'get notes' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/notities/ZAAK/$zaakProductaanvraag1Uuid"
            )
            Then(
                "the just created note should be returned"
            ) {
                response.isSuccessful shouldBe true
                val responseBody = response.body!!.string()
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [
                        {
                            "zaakUUID": "$zaakProductaanvraag1Uuid",
                            "tekst": "fakeNoteText",
                            "gebruikersnaamMedewerker": "$TEST_USER_1_USERNAME",
                            "voornaamAchternaamMedewerker": "$TEST_USER_1_NAME",
                            "bewerkenToegestaan": true
                        }
                    ]
                """.trimIndent()
            }
        }
    }
})
