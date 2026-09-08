/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.COORDINATOR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.config.TestUser
import nl.info.zac.itest.config.ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies activating zaakspecifieke autorisatie on an individual zaak from ZAC: the preconditions that
 * must hold, the fact that the marking cannot be lifted or the behandelaar changed while it stands, that
 * the behandelaar keeps access to their own marked zaak without holding the zaakspecifiek_geautoriseerd
 * role, and that a recordmanager who is granted that role through the PABC configuration reaches the zaak
 * as well - including a zaak whose behandelaar was removed outside ZAC.
 */
@Suppress("LargeClass")
class ZaakRestServiceZaakspecifiekeAutorisatieActivatieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    fun updateZaak(
        zaakUuid: UUID,
        zaakFields: String,
        testUser: TestUser
    ) = itestHttpClient.performPatchRequest(
        url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
        requestBodyAsString = """
            {
                "zaak": { $zaakFields },
                "reden": "fakeReason"
            }
        """.trimIndent(),
        testUser = testUser
    )

    fun markZaakspecifiekGeautoriseerd(zaakUuid: UUID, testUser: TestUser) =
        updateZaak(
            zaakUuid = zaakUuid,
            zaakFields = """"omschrijving": "itestOmschrijving", "isZaakspecifiekGeautoriseerd": true""",
            testUser = testUser
        )

    fun findZaakInWerklijst(zaakIdentificatie: String, testUser: TestUser) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zoeken/list",
            requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "ZAAK_IDENTIFICATIE": "$zaakIdentificatie" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "ZAAK"
                }
            """.trimIndent(),
            testUser = testUser
        )

    fun behandelaarRolUuid(zaakUuid: UUID): UUID? =
        JSONObject(openZaakClient.getRolesForZaak(zaakUuid).bodyAsString)
            .getJSONArray("results")
            .let { results ->
                (0 until results.length())
                    .map(results::getJSONObject)
                    .firstOrNull { it.getString("betrokkeneType") == "medewerker" }
                    ?.getString("uuid")
                    ?.run(UUID::fromString)
            }

    given("a zaak of a zaakspecifiek autoriseerbaar zaaktype with the logged-in user as behandelaar") {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            indexZaak = true,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )

        `when`("the behandelaar marks the zaak as zaakspecifiek geautoriseerd") {
            val response = markZaakspecifiekGeautoriseerd(zaakUuid, BEHANDELAAR_1)

            then("the zaak is marked and reports itself as zaakspecifiek geautoriseerd") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getBoolean("isZaakspecifiekGeautoriseerd") shouldBe true
            }

            and("the behandelaar still reads the zaak without holding the zaakspecifiek_geautoriseerd role") {
                with(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1)) {
                    code shouldBe HTTP_OK
                    JSONObject(bodyAsString).getJSONObject("rechten").getBoolean("lezen") shouldBe true
                }
            }

            and("the behandelaar still finds the zaak in a werklijst, because activation reindexes it") {
                eventually(30.seconds) {
                    with(findZaakInWerklijst(zaakIdentificatie, BEHANDELAAR_1)) {
                        code shouldBe HTTP_OK
                        JSONObject(bodyAsString).getInt("totaal") shouldBe 1
                    }
                }
            }

            and("a colleague with an application role for the zaaktype but no flag cannot open or find it") {
                zacClient.retrieveZaak(zaakUuid, COORDINATOR_1).code shouldBe HTTP_FORBIDDEN
                eventually(30.seconds) {
                    with(findZaakInWerklijst(zaakIdentificatie, COORDINATOR_1)) {
                        code shouldBe HTTP_OK
                        JSONObject(bodyAsString).getInt("totaal") shouldBe 0
                    }
                }
            }

            and("an employee holding the zaakspecifiek_geautoriseerd role reaches it") {
                zacClient.retrieveZaak(zaakUuid, ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1).code shouldBe HTTP_OK
            }

            and("a recordmanager granted that role through the PABC mapping reaches and finds it too") {
                zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1).code shouldBe HTTP_OK
                eventually(30.seconds) {
                    with(findZaakInWerklijst(zaakIdentificatie, RECORDMANAGER_1)) {
                        code shouldBe HTTP_OK
                        JSONObject(bodyAsString).getInt("totaal") shouldBe 1
                    }
                }
            }

            and("marking the already marked zaak again is accepted and creates no second marking") {
                markZaakspecifiekGeautoriseerd(zaakUuid, BEHANDELAAR_1).code shouldBe HTTP_OK
                val zaakeigenschappen = JSONArray(
                    openZaakClient.getZaakeigenschappenForZaak(zaakUuid).bodyAsString
                )
                (0 until zaakeigenschappen.length())
                    .map(zaakeigenschappen::getJSONObject)
                    .count { it.getString("naam") == "ZAAK_GEAUTORISEERD" } shouldBe 1
            }
        }

        `when`("an attempt is made to lift the marking") {
            val response = updateZaak(
                zaakUuid = zaakUuid,
                zaakFields = """"omschrijving": "itestOmschrijving", "isZaakspecifiekGeautoriseerd": false""",
                testUser = BEHANDELAAR_1
            )

            then("the request is refused with its own error code and the zaak stays marked") {
                response.code shouldBe HTTP_BAD_REQUEST
                JSONObject(response.bodyAsString).getString("message") shouldBe
                    "msg.error.zaakspecifieke.autorisatie.cannot.be.lifted"
                JSONObject(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1).bodyAsString)
                    .getBoolean("isZaakspecifiekGeautoriseerd") shouldBe true
            }
        }

        `when`("an attempt is made to release the zaak") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakUuid",
                        "groepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                        "reden": "fakeReason"
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            )

            then("the request is refused with its own error code and the zaak keeps its behandelaar") {
                response.code shouldBe HTTP_BAD_REQUEST
                JSONObject(response.bodyAsString).getString("message") shouldBe
                    "msg.error.zaakspecifiek.geautoriseerde.zaak.cannot.be.released"
                behandelaarRolUuid(zaakUuid).shouldNotBeNull()
            }
        }

        `when`("an attempt is made to assign the zaak to a different behandelaar") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/toekennen",
                requestBodyAsString = """
                    {
                        "zaakUUID": "$zaakUuid",
                        "groepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                        "behandelaarGebruikersnaam": "${ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1.username}",
                        "reden": "fakeReason"
                    }
                """.trimIndent(),
                testUser = ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
            )

            then("the request is refused with its own error code") {
                response.code shouldBe HTTP_BAD_REQUEST
                JSONObject(response.bodyAsString).getString("message") shouldBe
                    "msg.error.zaakspecifiek.geautoriseerde.zaak.cannot.be.reassigned"
            }
        }
    }

    given("a zaak of a zaaktype that is not zaakspecifiek autoriseerbaar") {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_1_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )

        `when`("the behandelaar tries to mark the zaak as zaakspecifiek geautoriseerd") {
            val response = markZaakspecifiekGeautoriseerd(zaakUuid, BEHANDELAAR_1)

            then("the request is refused with its own error code and the zaak is left unmarked") {
                response.code shouldBe HTTP_BAD_REQUEST
                JSONObject(response.bodyAsString).getString("message") shouldBe
                    "msg.error.zaaktype.not.zaakspecifiek-autoriseerbaar"
                JSONObject(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1).bodyAsString)
                    .getBoolean("isZaakspecifiekGeautoriseerd") shouldBe false
            }
        }
    }

    given("a zaak of a zaakspecifiek autoriseerbaar zaaktype without a behandelaar") {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1
        )

        `when`("an employee tries to mark the zaak as zaakspecifiek geautoriseerd") {
            val response = markZaakspecifiekGeautoriseerd(zaakUuid, BEHANDELAAR_1)

            then("the request is refused with its own error code and the zaak is left unmarked") {
                response.code shouldBe HTTP_BAD_REQUEST
                JSONObject(response.bodyAsString).getString("message") shouldBe
                    "msg.error.zaak.without.behandelaar.cannot.be.marked"
                JSONObject(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1).bodyAsString)
                    .getBoolean("isZaakspecifiekGeautoriseerd") shouldBe false
            }
        }
    }

    given("a zaak of a zaakspecifiek autoriseerbaar zaaktype whose behandelaar is someone else") {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )

        `when`(
            "an employee who is neither the behandelaar nor a flag holder tries to mark the zaak"
        ) {
            val response = markZaakspecifiekGeautoriseerd(zaakUuid, RECORDMANAGER_1)

            then("the request is refused, either by the policy or by the activation rule") {
                response.code shouldBeIn listOf(HTTP_BAD_REQUEST, HTTP_FORBIDDEN)
                JSONObject(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1).bodyAsString)
                    .getBoolean("isZaakspecifiekGeautoriseerd") shouldBe false
            }
        }
    }

    given("a zaakspecifiek geautoriseerde zaak whose behandelaar is removed outside ZAC") {
        val (_, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )
        markZaakspecifiekGeautoriseerd(zaakUuid, BEHANDELAAR_1).code shouldBe HTTP_OK
        behandelaarRolUuid(zaakUuid)?.let(openZaakClient::deleteRol)

        `when`("a recordmanager who holds the flag through the PABC mapping opens the zaak") {
            val response = zacClient.retrieveZaak(zaakUuid, RECORDMANAGER_1)

            then("the zaak is reachable, so it is not lost") {
                response.code shouldBe HTTP_OK
            }

            and("the recordmanager can assign a new behandelaar, who then reaches it without the flag") {
                itestHttpClient.performPatchRequest(
                    url = "$ZAC_API_URI/zaken/toekennen",
                    requestBodyAsString = """
                        {
                            "zaakUUID": "$zaakUuid",
                            "groepId": "${GROUP_BEHANDELAARS_TEST_1.name}",
                            "behandelaarGebruikersnaam": "${BEHANDELAAR_1.username}",
                            "reden": "fakeReason"
                        }
                    """.trimIndent(),
                    testUser = RECORDMANAGER_1
                ).code shouldBe HTTP_OK

                with(zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_1)) {
                    code shouldBe HTTP_OK
                    JSONObject(bodyAsString).getJSONObject("rechten").getBoolean("lezen") shouldBe true
                }
            }
        }
    }
})
