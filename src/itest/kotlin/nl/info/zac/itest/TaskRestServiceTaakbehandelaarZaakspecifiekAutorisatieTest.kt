/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_1_EN_BRP_ZOEKER_2
import nl.info.zac.itest.config.BEHANDELAAR_LONG_NAME_TEST
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_LONG_NAME_TEST
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestGroup
import nl.info.zac.itest.config.TestUser
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class TaskRestServiceTaakbehandelaarZaakspecifiekAutorisatieTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    fun zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid: UUID): List<String> =
        JSONObject(openZaakClient.getRolesForZaak(zaakUuid).bodyAsString)
            .getJSONArray("results")
            .let { results -> (0 until results.length()).map(results::getJSONObject) }
            .filter { it.getString("omschrijving") == ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER }
            .map { it.getJSONObject("betrokkeneIdentificatie").getString("identificatie") }

    fun markZaakspecifiekGeautoriseerd(zaakUuid: UUID) =
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
            requestBodyAsString = """
                {
                    "zaak": { "omschrijving": "itestOmschrijving", "isZaakspecifiekGeautoriseerd": true },
                    "reden": "fakeReason"
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1
        ).code shouldBe HTTP_OK

    fun assignTask(taskId: String, zaakUuid: UUID, group: TestGroup, behandelaar: TestUser?) =
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/taken/toekennen",
            requestBodyAsString = """
                {
                    "taakId": "$taskId",
                    "zaakUuid": "$zaakUuid",
                    "groepId": "${group.name}",
                    "behandelaarId": ${behandelaar?.let { "\"${it.username}\"" } ?: "null"},
                    "reden": "fakeReason"
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1
        ).code shouldBe HTTP_NO_CONTENT

    fun findZaak(zaakIdentificatie: String, testUser: TestUser) =
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

    given(
        """
        A zaakspecifiek geautoriseerde CMMN zaak whose zaakbehandelaar starts a taak for a behandelaar
        who does not hold the zaakspecifiek_geautoriseerd application role
        """
    ) {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )
        markZaakspecifiekGeautoriseerd(zaakUuid)
        val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentificatie,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            medewerker = BEHANDELAAR_1_EN_BRP_ZOEKER_2,
            testUser = BEHANDELAAR_1
        )

        `when`("the zaak and the taak are read by that taakbehandelaar") {
            val zaakResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
                testUser = BEHANDELAAR_1_EN_BRP_ZOEKER_2
            )
            val taskResponse = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/$taskId",
                testUser = BEHANDELAAR_1_EN_BRP_ZOEKER_2
            )

            then("the taakbehandelaar is a zaakspecifiek geautoriseerde medewerker of the zaak in Open Zaak") {
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldBe
                    listOf(BEHANDELAAR_1_EN_BRP_ZOEKER_2.username)
            }

            and("the taakbehandelaar can read both the zaak and the taak") {
                zaakResponse.code shouldBe HTTP_OK
                taskResponse.code shouldBe HTTP_OK
            }

            and("the zaakbehandelaar is not stored as zaakspecifiek geautoriseerde medewerker") {
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldNotContain BEHANDELAAR_1.username
            }
        }

        `when`("the taakhistorie and the betrokkenen of the zaak are read") {
            val historyLabels = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/$taskId/historie",
                testUser = BEHANDELAAR_1
            ).let { response ->
                response.code shouldBe HTTP_OK
                JSONArray(response.bodyAsString).let { lines ->
                    (0 until lines.length()).map { lines.getJSONObject(it).getString("attribuutLabel") }
                }
            }
            val betrokkeneRoltypen = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/betrokkene",
                testUser = BEHANDELAAR_1
            ).let { response ->
                response.code shouldBe HTTP_OK
                JSONArray(response.bodyAsString).let { betrokkenen ->
                    (0 until betrokkenen.length()).map { betrokkenen.getJSONObject(it).getString("roltype") }
                }
            }

            then("the taakhistorie shows that a zaakspecifiek geautoriseerde medewerker was added") {
                historyLabels shouldContain ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
            }

            and("the taakbehandelaar is not listed as a betrokkene") {
                betrokkeneRoltypen shouldNotContain ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
            }
        }

        `when`("the taakbehandelaar searches for the zaak") {
            then("the zaak is found") {
                eventually(30.seconds) {
                    findZaak(zaakIdentificatie, BEHANDELAAR_1_EN_BRP_ZOEKER_2).let {
                        it.code shouldBe HTTP_OK
                        JSONObject(it.bodyAsString).getInt("totaal") shouldBe 1
                    }
                }
            }
        }

        `when`("the taak is reassigned to another behandelaar") {
            assignTask(
                taskId = taskId,
                zaakUuid = zaakUuid,
                group = GROUP_BEHANDELAARS_LONG_NAME_TEST,
                behandelaar = BEHANDELAAR_LONG_NAME_TEST
            )

            then("both the previous and the new taakbehandelaar are zaakspecifiek geautoriseerde medewerkers") {
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid).sorted() shouldBe listOf(
                    BEHANDELAAR_1_EN_BRP_ZOEKER_2.username,
                    BEHANDELAAR_LONG_NAME_TEST.username
                ).sorted()
            }
        }

        `when`("the taak is released") {
            assignTask(
                taskId = taskId,
                zaakUuid = zaakUuid,
                group = GROUP_BEHANDELAARS_LONG_NAME_TEST,
                behandelaar = null
            )

            then("the released taakbehandelaar can still read the zaak") {
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldContain BEHANDELAAR_LONG_NAME_TEST.username
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
                    testUser = BEHANDELAAR_LONG_NAME_TEST
                ).code shouldBe HTTP_OK
            }
        }

        `when`("the taak is assigned back to the first taakbehandelaar") {
            assignTask(
                taskId = taskId,
                zaakUuid = zaakUuid,
                group = GROUP_BEHANDELAARS_TEST_1,
                behandelaar = BEHANDELAAR_1_EN_BRP_ZOEKER_2
            )

            then("each taakbehandelaar is stored exactly once as zaakspecifiek geautoriseerde medewerker") {
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid).sorted() shouldBe listOf(
                    BEHANDELAAR_1_EN_BRP_ZOEKER_2.username,
                    BEHANDELAAR_LONG_NAME_TEST.username
                ).sorted()
            }
        }
    }

    given(
        """
        A CMMN zaak that is not yet zaakspecifiek geautoriseerd with a taak assigned to a behandelaar who does
        not hold the zaakspecifiek_geautoriseerd application role
        """
    ) {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )
        val taskId = taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentificatie,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            medewerker = BEHANDELAAR_1_EN_BRP_ZOEKER_2,
            testUser = BEHANDELAAR_1
        )
        val zaakspecifiekGeautoriseerdeMedewerkerIdsBeforeMarking = zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid)

        `when`("the zaak is marked as zaakspecifiek geautoriseerd") {
            markZaakspecifiekGeautoriseerd(zaakUuid)

            then("the taakbehandelaar becomes a zaakspecifiek geautoriseerde medewerker only once the zaak is marked") {
                zaakspecifiekGeautoriseerdeMedewerkerIdsBeforeMarking shouldBe emptyList()
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldBe
                    listOf(BEHANDELAAR_1_EN_BRP_ZOEKER_2.username)
            }

            and("the taakbehandelaar can still read the taak") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/taken/$taskId",
                    testUser = BEHANDELAAR_1_EN_BRP_ZOEKER_2
                ).code shouldBe HTTP_OK
            }
        }
    }
})
