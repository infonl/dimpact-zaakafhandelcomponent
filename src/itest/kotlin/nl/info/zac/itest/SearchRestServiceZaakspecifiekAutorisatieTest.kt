/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.DocumentHelper
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.OpenZaakClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.TestUser
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that once a zaak of a zaaktype that supports zaakspecifieke autorisatie is marked as
 * zaakspecifiek geautoriseerd, and the search index is refreshed to reflect that, the zaak (and its
 * task and document) disappear from werklijst/zoekresultaat searches for a behandelaar who lacks the
 * zaakspecifiek_geautoriseerd application role, while remaining visible, with correct rechten, for a
 * behandelaar who holds it.
 */
class SearchRestServiceZaakspecifiekAutorisatieTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)
    val taskHelper = TaskHelper(zacClient)
    val documentHelper = DocumentHelper(zacClient)
    val openZaakClient = OpenZaakClient(itestHttpClient)

    fun searchZaak(zaakIdentificatie: String, testUser: TestUser) =
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

    fun searchTaak(zaakIdentificatie: String, testUser: TestUser) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zoeken/list",
            requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "TAAK_ZAAK_ID": "$zaakIdentificatie" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "TAAK"
                }
            """.trimIndent(),
            testUser = testUser
        )

    fun searchDocument(documentTitle: String, testUser: TestUser) =
        itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zoeken/list",
            requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": false,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "DOCUMENT_TITEL": "$documentTitle" },
                    "filters": {},
                    "datums": {},
                    "rows": 10,
                    "page": 0,
                    "type": "DOCUMENT"
                }
            """.trimIndent(),
            testUser = testUser
        )

    given(
        """
        A CMMN zaak of a zaaktype that supports zaakspecifieke autorisatie has been created and indexed,
        with a task and a document added to it, and the zaak is then marked as zaakspecifiek
        geautoriseerd and the search index is refreshed to reflect this
        """
    ) {
        val documentTitle = "itestDocumentTitle-${System.currentTimeMillis()}"
        val (zaakIdentificatie, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_CMMN_TEST_2_UUID,
            indexZaak = true,
            testUser = BEHANDELAAR_1
        )
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
        openZaakClient.createZaakeigenschap(
            zaakUUID = zaakUuid,
            zaaktypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            eigenschapNaam = "ZAAK_GEAUTORISEERD",
            waarde = "true"
        )
        // the zaakeigenschap notificatie triggered by createZaakeigenschap() above is handled
        // asynchronously and reindexes the zaak, its (open) taken and its documenten; wait for that to
        // complete by polling for the actual effect of the newly set flag: the zaak, its task and its
        // document becoming invisible to a behandelaar who lacks the zaakspecifiek_geautoriseerd role.
        // Polling the flagged user's view is not a valid signal here, since that user can see the zaak
        // regardless of whether reindexing has finished.
        eventually(30.seconds) {
            JSONObject(searchZaak(zaakIdentificatie, BEHANDELAAR_1).bodyAsString).getInt("totaal") shouldBe 0
            JSONObject(searchTaak(zaakIdentificatie, BEHANDELAAR_1).bodyAsString).getInt("totaal") shouldBe 0
            JSONObject(searchDocument(documentTitle, BEHANDELAAR_1).bodyAsString).getInt("totaal") shouldBe 0
        }

        `when`(
            "worklist/search results are requested by a behandelaar authorized for the zaaktype but " +
                "without the zaakspecifiek_geautoriseerd application role"
        ) {
            val zaakResponse = searchZaak(zaakIdentificatie, BEHANDELAAR_1)
            val taakResponse = searchTaak(zaakIdentificatie, BEHANDELAAR_1)
            val documentResponse = searchDocument(documentTitle, BEHANDELAAR_1)

            then("the zaakspecifiek geautoriseerde zaak, its task and its document are absent from all results") {
                logger.info { "Zaak search response: ${zaakResponse.bodyAsString}" }
                logger.info { "Taak search response: ${taakResponse.bodyAsString}" }
                logger.info { "Document search response: ${documentResponse.bodyAsString}" }
                zaakResponse.code shouldBe HTTP_OK
                taakResponse.code shouldBe HTTP_OK
                documentResponse.code shouldBe HTTP_OK
                JSONObject(zaakResponse.bodyAsString).getInt("totaal") shouldBe 0
                JSONObject(taakResponse.bodyAsString).getInt("totaal") shouldBe 0
                JSONObject(documentResponse.bodyAsString).getInt("totaal") shouldBe 0
            }
        }

        `when`("worklist/search results are requested by a user holding the zaakspecifiek_geautoriseerd role") {
            val zaakResponse = searchZaak(zaakIdentificatie, ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1)
            val taakResponse = searchTaak(zaakIdentificatie, ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1)
            val documentResponse = searchDocument(documentTitle, ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1)

            then(
                "the zaakspecifiek geautoriseerde zaak, its task and its document are present in all " +
                    "results with lezen rechten set to true"
            ) {
                logger.info { "Zaak search response: ${zaakResponse.bodyAsString}" }
                logger.info { "Taak search response: ${taakResponse.bodyAsString}" }
                logger.info { "Document search response: ${documentResponse.bodyAsString}" }
                zaakResponse.code shouldBe HTTP_OK
                taakResponse.code shouldBe HTTP_OK
                documentResponse.code shouldBe HTTP_OK

                val zaakResult = JSONObject(zaakResponse.bodyAsString)
                zaakResult.getInt("totaal") shouldBe 1
                zaakResult.getJSONArray("resultaten").getJSONObject(0)
                    .getJSONObject("rechten").getBoolean("lezen") shouldBe true

                val taakResult = JSONObject(taakResponse.bodyAsString)
                taakResult.getInt("totaal") shouldBe 1
                taakResult.getJSONArray("resultaten").getJSONObject(0)
                    .getJSONObject("rechten").getBoolean("lezen") shouldBe true

                val documentResult = JSONObject(documentResponse.bodyAsString)
                documentResult.getInt("totaal") shouldBe 1
                documentResult.getJSONArray("resultaten").getJSONObject(0)
                    .getJSONObject("rechten").getBoolean("lezen") shouldBe true
            }
        }
    }
})
