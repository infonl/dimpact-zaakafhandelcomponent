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
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject

@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class NoteServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    beforeSpec {
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)
    }

    afterSpec {
        // re-authenticate as beheerder since currently subsequent integration tests rely on this user being logged in
        authenticate(BEHEERDER_ELK_ZAAKTYPE)
    }

    Given("An existing zaak") {
        When(
            """
            a note is created with the provided username equal to the username of the currently logged in user
            """
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notities",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakProductaanvraag1Uuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "${BEHANDELAAR_DOMAIN_TEST_1.username}"
                    }
                """.trimIndent()
            )
            Then(
                "the created note and related metadata should be returned, with the 'editing allowed' flag set to true"
            ) {
                response.isSuccessful shouldBe true
                val responseBody = response.body.string()
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                        "zaakUUID": "$zaakProductaanvraag1Uuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "${BEHANDELAAR_DOMAIN_TEST_1.username}",
                        "voornaamAchternaamMedewerker": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
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
                url = "$ZAC_API_URI/notities/zaken/$zaakProductaanvraag1Uuid"
            )
            Then(
                "the just created note should be returned"
            ) {
                response.isSuccessful shouldBe true
                val responseBody = response.body.string()
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [
                        {
                            "zaakUUID": "$zaakProductaanvraag1Uuid",
                            "tekst": "fakeNoteText",
                            "gebruikersnaamMedewerker": "${BEHANDELAAR_DOMAIN_TEST_1.username}",
                            "voornaamAchternaamMedewerker": "${BEHANDELAAR_DOMAIN_TEST_1.displayName}",
                            "bewerkenToegestaan": true
                        }
                    ]
                """.trimIndent()
            }
        }
    }
})
