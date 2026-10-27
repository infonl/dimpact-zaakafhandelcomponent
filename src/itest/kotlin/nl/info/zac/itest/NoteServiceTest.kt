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
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_2
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID

class NoteServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)

    given("An existing zaak and a logged-in behandelaar") {
        lateinit var zaakUuid: UUID
        var noteId = 0L
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_3_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid").run(UUID::fromString)
            }
        }

        `when`("a note is created with the username of another user as author") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/notities",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakUuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "${COORDINATOR_1.username}"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            )
            then(
                """
                the created note should be returned with the logged-in user as author,
                and with the 'editing allowed' flag set to true
                """
            ) {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    {
                        "zaakUUID": "$zaakUuid",
                        "tekst": "fakeNoteText",
                        "gebruikersnaamMedewerker": "${BEHANDELAAR_1.username}",
                        "voornaamAchternaamMedewerker": "${BEHANDELAAR_1.displayName}",
                        "isBewerkenToegestaan": true
                    }
                """.trimIndent()
                with(JSONObject(responseBody)) {
                    get("tijdstipLaatsteWijziging") shouldNotBe null
                    noteId = getLong("id")
                }
            }
        }

        `when`("the 'get notes' endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/notities/zaken/$zaakUuid",
                testUser = BEHANDELAAR_1
            )
            then(
                "the just created note should be returned"
            ) {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                responseBody shouldEqualJsonIgnoringExtraneousFields """
                    [
                        {
                            "zaakUUID": "$zaakUuid",
                            "tekst": "fakeNoteText",
                            "gebruikersnaamMedewerker": "${BEHANDELAAR_1.username}",
                            "voornaamAchternaamMedewerker": "${BEHANDELAAR_1.displayName}",
                            "isBewerkenToegestaan": true
                        }
                    ]
                """.trimIndent()
            }
        }

        `when`("another user who may read the zaak updates the note") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/notities",
                requestBodyAsString = """
                    {
                        "id": $noteId,
                        "zaakUUID": "$zaakUuid",
                        "tekst": "fakeChangedNoteText",
                        "gebruikersnaamMedewerker": "${COORDINATOR_1.username}"
                    }
                """.trimIndent(),
                testUser = COORDINATOR_1
            )

            then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }

        `when`("another user who may read the zaak deletes the note") {
            val response = itestHttpClient.performDeleteRequest(
                url = "$ZAC_API_URI/notities/$noteId",
                testUser = COORDINATOR_1
            )

            then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }

        `when`("a user who may not read the zaak calls the 'get notes' endpoint") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/notities/zaken/$zaakUuid",
                testUser = BEHANDELAAR_2
            )

            then("the response should be forbidden") {
                response.code shouldBe HTTP_FORBIDDEN
            }
        }

        `when`("the author calls the 'get notes' endpoint after the forbidden update and delete") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/notities/zaken/$zaakUuid",
                testUser = BEHANDELAAR_1
            )

            then("the note should be returned unchanged") {
                response.code shouldBe HTTP_OK
                response.bodyAsString shouldEqualJsonIgnoringExtraneousFields """
                    [
                        {
                            "id": $noteId,
                            "zaakUUID": "$zaakUuid",
                            "tekst": "fakeNoteText",
                            "gebruikersnaamMedewerker": "${BEHANDELAAR_1.username}"
                        }
                    ]
                """.trimIndent()
            }
        }
    }
})
