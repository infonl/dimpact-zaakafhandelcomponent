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
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.DocumentHelper
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
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestGroup
import nl.info.zac.itest.config.TestUser
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZaakspecifiekeAutorisatieRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    fun zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid: UUID): List<String> =
        JSONObject(openZaakClient.getRolesForZaak(zaakUuid).bodyAsString)
            .getJSONArray("results")
            .let { results -> (0 until results.length()).map(results::getJSONObject) }
            .filter { it.getString("omschrijving") == ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER }
            .map { it.getJSONObject("betrokkeneIdentificatie").getString("identificatie") }

    fun listKandidaten(zaakUuid: UUID, group: TestGroup, testUser: TestUser): List<String> =
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/zaakspecifiek-geautoriseerde-medewerkers/kandidaten" +
                "?groepId=${group.name}",
            testUser = testUser
        ).let { response ->
            response.code shouldBe HTTP_OK
            JSONArray(response.bodyAsString).let { users ->
                (0 until users.length()).map { users.getJSONObject(it).getString("id") }
            }
        }

    fun addMedewerker(zaakUuid: UUID, group: TestGroup, medewerker: TestUser, testUser: TestUser) =
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/zaakspecifiek-geautoriseerde-medewerkers",
            requestBodyAsString = """
                { "groepId": "${group.name}", "medewerkerId": "${medewerker.username}" }
            """.trimIndent(),
            testUser = testUser
        )

    fun searchTotalForAddedMedewerker(type: String, zoekveld: String, zoekwaarde: String) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zoeken/list",
            requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "$zoekveld": "$zoekwaarde" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "$type"
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1_EN_BRP_ZOEKER_2
        ).let {
            it.code shouldBe HTTP_OK
            JSONObject(it.bodyAsString).getInt("totaal")
        }

    fun readZaak(zaakUuid: UUID, testUser: TestUser) =
        itestHttpClient.performGetRequest(url = "$ZAC_API_URI/zaken/zaak/$zaakUuid", testUser = testUser)

    given(
        """
        A zaakspecifiek geautoriseerde CMMN zaak with zaakbehandelaar BEHANDELAAR_1, and BEHANDELAAR_1_EN_BRP_ZOEKER_2
        in the same behandelaar groep without the zaakspecifiek_geautoriseerd application role
        """
    ) {
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1,
            behandelaarId = BEHANDELAAR_1.username,
            behandelaarName = BEHANDELAAR_1.displayName
        )
        val documentTitle = "itestZaakspecifiekDocument-${System.currentTimeMillis()}"
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentificatie,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            waitForTaskToBeIndexed = true,
            testUser = BEHANDELAAR_1
        )
        documentHelper.uploadDocumentToZaak(
            zaakUuid = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            documentTitle = documentTitle,
            authorName = FAKE_AUTHOR_NAME,
            indexDocument = true,
            testUser = BEHANDELAAR_1
        )
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

        `when`("the zaakbehandelaar lists the kandidaten of the behandelaar groep") {
            val kandidaten = listKandidaten(zaakUuid, GROUP_BEHANDELAARS_TEST_1, BEHANDELAAR_1)

            then("only the medewerker without access is returned") {
                kandidaten shouldBe listOf(BEHANDELAAR_1_EN_BRP_ZOEKER_2.username)
            }

            and("that medewerker cannot read the zaak yet") {
                readZaak(zaakUuid, BEHANDELAAR_1_EN_BRP_ZOEKER_2).code shouldBe HTTP_FORBIDDEN
            }
        }

        `when`("the kandidaten are requested without a groep") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/zaakspecifiek-geautoriseerde-medewerkers/kandidaten",
                testUser = BEHANDELAAR_1
            )

            then("the request is refused as invalid") {
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }

        `when`("the zaakbehandelaar adds that medewerker") {
            val response = addMedewerker(
                zaakUuid = zaakUuid,
                group = GROUP_BEHANDELAARS_TEST_1,
                medewerker = BEHANDELAAR_1_EN_BRP_ZOEKER_2,
                testUser = BEHANDELAAR_1
            )

            then("the medewerker is a zaakspecifiek geautoriseerde medewerker of the zaak in Open Zaak") {
                response.code shouldBe HTTP_OK
                JSONObject(response.bodyAsString).getString("id") shouldBe BEHANDELAAR_1_EN_BRP_ZOEKER_2.username
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldBe
                    listOf(BEHANDELAAR_1_EN_BRP_ZOEKER_2.username)
            }

            and("the medewerker can read the zaak and finds the zaak, its taak and its document in the werkvoorraden") {
                readZaak(zaakUuid, BEHANDELAAR_1_EN_BRP_ZOEKER_2).code shouldBe HTTP_OK
                eventually(30.seconds) {
                    searchTotalForAddedMedewerker("ZAAK", "ZAAK_IDENTIFICATIE", zaakIdentificatie) shouldBe 1
                    searchTotalForAddedMedewerker("TAAK", "TAAK_ZAAK_ID", zaakIdentificatie) shouldBe 1
                    searchTotalForAddedMedewerker("DOCUMENT", "DOCUMENT_TITEL", documentTitle) shouldBe 1
                }
            }

            and("the medewerker is not listed as a betrokkene") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/betrokkene",
                    testUser = BEHANDELAAR_1
                ).let { betrokkeneResponse ->
                    betrokkeneResponse.code shouldBe HTTP_OK
                    JSONArray(betrokkeneResponse.bodyAsString).let { betrokkenen ->
                        (0 until betrokkenen.length()).map { betrokkenen.getJSONObject(it).getString("roltype") }
                    } shouldNotContain ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
                }
            }

            and("the zaakhistorie shows that a zaakspecifiek geautoriseerde medewerker was added") {
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/historie",
                    testUser = BEHANDELAAR_1
                ).let { historieResponse ->
                    historieResponse.code shouldBe HTTP_OK
                    JSONArray(historieResponse.bodyAsString).let { lines ->
                        (0 until lines.length()).map { lines.getJSONObject(it).getString("attribuutLabel") }
                    } shouldContain ROLTYPE_NAME_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
                }
            }

            and("the medewerker is no longer a kandidaat") {
                listKandidaten(zaakUuid, GROUP_BEHANDELAARS_TEST_1, BEHANDELAAR_1) shouldBe emptyList()
            }
        }

        `when`("the same medewerker is added again") {
            val response = addMedewerker(
                zaakUuid = zaakUuid,
                group = GROUP_BEHANDELAARS_TEST_1,
                medewerker = BEHANDELAAR_1_EN_BRP_ZOEKER_2,
                testUser = BEHANDELAAR_1
            )

            then("the request is refused and no second rol is added") {
                response.code shouldBe HTTP_BAD_REQUEST
                response.bodyAsString shouldContain "msg.error.medewerker.already.zaakspecifiek.geautoriseerd"
                zaakspecifiekGeautoriseerdeMedewerkerIds(zaakUuid) shouldBe
                    listOf(BEHANDELAAR_1_EN_BRP_ZOEKER_2.username)
            }
        }

        `when`("the added medewerker adds a medewerker of another behandelaar groep") {
            val response = addMedewerker(
                zaakUuid = zaakUuid,
                group = GROUP_BEHANDELAARS_LONG_NAME_TEST,
                medewerker = BEHANDELAAR_LONG_NAME_TEST,
                testUser = BEHANDELAAR_1_EN_BRP_ZOEKER_2
            )

            then("that medewerker can read the zaak as well") {
                response.code shouldBe HTTP_OK
                readZaak(zaakUuid, BEHANDELAAR_LONG_NAME_TEST).code shouldBe HTTP_OK
            }
        }
    }
})
