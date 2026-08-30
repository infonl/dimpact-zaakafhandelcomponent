/*
 * SPDX-FileCopyrightText: 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.VERTROUWELIJKHEIDS_AANDUIDING_ZAAKVERTROUWELIJK
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
            vertrouwelijkheidaanduiding = VERTROUWELIJKHEIDS_AANDUIDING_ZAAKVERTROUWELIJK,
            testUser = BEHEERDER_1
        )

        `when`("""the internal ZAC reindexing endpoint is called for type 'zaak'""") {
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
                    val logs = zacContainerLogs()
                    logs shouldContain "[ZAAK] Reindexing started. Solr index currently contains"
                    logs shouldContain "[ZAAK] Reindexing finished. Reindexed:"
                    logs shouldContain "not reindexed because of errors:"
                }
            }
        }
        `when`("""the reindexing endpoint is called for type 'task'""") {
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
                    val logs = zacContainerLogs()
                    logs shouldContain "[TAAK] Reindexing started. Solr index currently contains"
                    logs shouldContain "[TAAK] Reindexing finished. Reindexed:"
                    logs shouldContain "not reindexed because of errors:"
                }
            }
        }
        `when`("""the reindexing endpoint is called for type 'document'""") {
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
                    val logs = zacContainerLogs()
                    logs shouldContain "[DOCUMENT] Reindexing started. Solr index currently contains"
                    logs shouldContain "[DOCUMENT] Reindexing finished. Reindexed:"
                    logs shouldContain "not reindexed because of errors:"
                }
            }
        }
        `when`("""the internal ZAC "reindex everything" endpoint is called""") {
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
                    val logs = zacContainerLogs()
                    logs shouldContain "Complete reindexing process started for object types:"
                    logs shouldContain "Complete reindexing process finished for object types:"
                    listOf("ZAAK", "TAAK", "DOCUMENT").forEach { zoekObjectType ->
                        logs shouldContain "[$zoekObjectType] Reindexing started. Solr index currently contains"
                        logs shouldContain "[$zoekObjectType] Reindexing finished"
                    }
                }
            }
        }
    }
})
