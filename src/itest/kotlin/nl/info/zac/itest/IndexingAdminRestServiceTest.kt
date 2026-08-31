/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.VERTROUWELIJKHEIDAANDUIDING_ZAAKVERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import nl.info.zac.itest.config.dockerComposeContainer
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private fun zacContainerLogs(): String =
    dockerComposeContainer.getContainerByServiceName(ZAC_CONTAINER_SERVICE_NAME).get().logs

/**
 * ZAC's own startup already triggers a full `reindexAll()` when the Solr schema is not yet at
 * its latest version (e.g. a fresh Solr core, such as in CI). Asserting against the container's
 * entire accumulated log would then make these assertions pass regardless of whether the request
 * fired by this test actually worked, since the startup reindex's log lines are already present
 * before that request is ever sent. Diffing against a log snapshot taken right before the request
 * ensures each assertion only matches lines the request under test actually caused.
 */
private fun String.shouldContainLogLineMatching(regex: Regex) {
    withClue(
        "expected the ZAC container log lines logged since the request to contain a line matching: " +
            "${regex.pattern}\n\nLog lines logged since the request:\n$this"
    ) {
        regex.containsMatchIn(this) shouldBe true
    }
}

/**
 * Unlike [String.removePrefix], which silently returns the receiver unchanged when [previousLogs] is
 * not actually a prefix, this fails loudly - so a future change that breaks the prefix guarantee (e.g.
 * a Testcontainers upgrade reordering log frames) surfaces as a test failure instead of the assertions
 * below silently matching against the full accumulated container log again.
 */
private fun String.newLogsSince(previousLogs: String): String {
    this shouldStartWith previousLogs
    return substring(previousLogs.length)
}

private fun reindexingStartedRegex(zoekObjectType: String) = Regex(
    """\[$zoekObjectType] Reindexing started\. Solr index currently contains (?:\d+|unknown) """ +
        """documents of type '$zoekObjectType'\."""
)

private fun reindexingFinishedRegex(zoekObjectType: String) = Regex(
    """\[$zoekObjectType] Reindexing finished\. Reindexed: \d+ / \d+, skipped: \d+, """ +
        """not reindexed because of errors: \d+\. Solr index contains (?:\d+|unknown) """ +
        """documents of type '$zoekObjectType'\."""
)

class IndexingAdminRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val taskHelper = TaskHelper(zacClient)

    given("A zaak, a task and a document have been created, and a beheerder is logged in") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentification: String
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_3_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHEERDER_1
        ).run {
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid").let(UUID::fromString)
                zaakIdentification = getString("identificatie")
            }
        }
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHEERDER_1
        )
        zacClient.createEnkelvoudigInformatieobjectForZaak(
            zaakUUID = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            fileMediaType = PDF_MIME_TYPE,
            vertrouwelijkheidaanduiding = VERTROUWELIJKHEIDAANDUIDING_ZAAKVERTROUWELIJK,
            testUser = BEHEERDER_1
        )

        `when`("""the internal ZAC reindexing endpoint is called for type 'zaak'""") {
            val logsBeforeReindex = zacContainerLogs()
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/indexeren/herindexeren/ZAAK",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            then(
                """the response is successful and at least one zaak is indexed"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT
                // wait for the indexing to complete
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                           {
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": false,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": {},
                            "filters": {},
                            "datums": {},
                            "rows": 100,
                            "page": 0,
                            "type": "ZAAK"
                        }
                        """.trimIndent(),
                        testUser = BEHEERDER_1
                    )
                    JSONObject(response.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                }
            }

            and("the ZAC log reports that zaken reindexing started, finished and its reindex summary") {
                eventually(10.seconds) {
                    val newLogs = zacContainerLogs().newLogsSince(logsBeforeReindex)
                    newLogs.shouldContainLogLineMatching(reindexingStartedRegex("ZAAK"))
                    newLogs.shouldContainLogLineMatching(reindexingFinishedRegex("ZAAK"))
                }
            }
        }
        `when`("""the reindexing endpoint is called for type 'task'""") {
            val logsBeforeReindex = zacContainerLogs()
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/indexeren/herindexeren/TAAK",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            then(
                """the response is successful and at least one task is indexed"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT
                // wait for the indexing to complete
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                           {
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": false,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": {},
                            "filters": {},
                            "datums": {},
                            "rows": 100,
                            "page": 0,
                            "type": "TAAK"
                        }
                        """.trimIndent(),
                        testUser = BEHEERDER_1
                    )
                    JSONObject(response.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                }
            }

            and("the ZAC log reports that taken reindexing started, finished and its reindex summary") {
                eventually(10.seconds) {
                    val newLogs = zacContainerLogs().newLogsSince(logsBeforeReindex)
                    newLogs.shouldContainLogLineMatching(reindexingStartedRegex("TAAK"))
                    newLogs.shouldContainLogLineMatching(reindexingFinishedRegex("TAAK"))
                }
            }
        }
        `when`("""the reindexing endpoint is called for type 'document'""") {
            val logsBeforeReindex = zacContainerLogs()
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/internal/indexeren/herindexeren/DOCUMENT",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            then(
                """the response is successful and at least one document is indexed"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT
                // wait for the indexing to complete
                eventually(10.seconds) {
                    val response = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                           {
                            "alleenMijnZaken": false,
                            "alleenOpenstaandeZaken": false,
                            "alleenAfgeslotenZaken": false,
                            "alleenMijnTaken": false,
                            "zoeken": {},
                            "filters": {},
                            "datums": {},
                            "rows": 100,
                            "page": 0,
                            "type": "DOCUMENT"
                        }
                        """.trimIndent(),
                        testUser = BEHEERDER_1
                    )
                    JSONObject(response.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                }
            }

            and("the ZAC log reports that documenten reindexing started, finished and its reindex summary") {
                eventually(10.seconds) {
                    val newLogs = zacContainerLogs().newLogsSince(logsBeforeReindex)
                    newLogs.shouldContainLogLineMatching(reindexingStartedRegex("DOCUMENT"))
                    newLogs.shouldContainLogLineMatching(reindexingFinishedRegex("DOCUMENT"))
                }
            }
        }
        `when`("""the internal ZAC "reindex everything" endpoint is called""") {
            val logsBeforeReindex = zacContainerLogs()
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/indexeren/herindexeren",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            then(
                """the response is successful and zaken, taken and documenten are all reindexed"""
            ) {
                response.code shouldBe HTTP_NO_CONTENT
                listOf("ZAAK", "TAAK", "DOCUMENT").forEach { zoekObjectType ->
                    eventually(10.seconds) {
                        val searchResponse = itestHttpClient.performPutRequest(
                            url = "$ZAC_API_URI/zoeken/list",
                            requestBodyAsString = """
                               {
                                "alleenMijnZaken": false,
                                "alleenOpenstaandeZaken": false,
                                "alleenAfgeslotenZaken": false,
                                "alleenMijnTaken": false,
                                "zoeken": {},
                                "filters": {},
                                "datums": {},
                                "rows": 100,
                                "page": 0,
                                "type": "$zoekObjectType"
                            }
                            """.trimIndent(),
                            testUser = BEHEERDER_1
                        )
                        JSONObject(searchResponse.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                    }
                }
            }

            and("the ZAC log reports the complete reindexing process and the Solr index counts") {
                eventually(10.seconds) {
                    val newLogs = zacContainerLogs().newLogsSince(logsBeforeReindex)
                    newLogs.shouldContainLogLineMatching(
                        Regex("""Complete reindexing process started for object types: \[TAAK, ZAAK, DOCUMENT]""")
                    )
                    newLogs.shouldContainLogLineMatching(
                        Regex("""Complete reindexing process finished for object types: \[TAAK, ZAAK, DOCUMENT]""")
                    )
                    listOf("ZAAK", "TAAK", "DOCUMENT").forEach { zoekObjectType ->
                        newLogs.shouldContainLogLineMatching(reindexingStartedRegex(zoekObjectType))
                        newLogs.shouldContainLogLineMatching(reindexingFinishedRegex(zoekObjectType))
                    }
                }
            }
        }
    }
})
