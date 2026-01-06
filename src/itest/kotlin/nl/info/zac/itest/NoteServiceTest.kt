/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

class NoteServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)

    Given("An existing zaak and a logged-in behandelaar") {
        lateinit var zaakUuid: UUID
        authenticate(BEHANDELAAR_DOMAIN_TEST_1)
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_3_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01
        ).run {
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid").run(UUID::fromString)
            }
        }

        When(
            """
            a note is created with the provided username equal to the username of the currently logged in user
            """
        ) {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notities",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakUuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "${BEHANDELAAR_DOMAIN_TEST_1.username}"
                    }
                """.trimIndent()
            )
            Then(
                "the created note and related metadata should be returned, with the 'editing allowed' flag set to true"
            ) {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                        "zaakUUID": "$zaakUuid",
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
                url = "$ZAC_API_URI/notities/zaken/$zaakUuid"
            )
            Then(
                "the just created note should be returned"
            ) {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [
                        {
                            "zaakUUID": "$zaakUuid",
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
