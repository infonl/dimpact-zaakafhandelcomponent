/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK
import nl.info.zac.itest.config.ItestConfiguration.PDF_MIME_TYPE
import nl.info.zac.itest.config.ItestConfiguration.TEST_PDF_FILE_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class IndexingAdminRestServiceTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val taskHelper = TaskHelper(zacClient)

    Given("A zaak, a task and a document have been created, and a beheerder is logged in") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentification: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_3_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHEERDER_ELK_ZAAKTYPE
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
            group = BEHANDELAARS_DOMAIN_TEST_1,
            testUser = BEHEERDER_ELK_ZAAKTYPE
        )
        zacClient.createEnkelvoudigInformatieobjectForZaak(
            zaakUUID = zaakUuid,
            fileName = TEST_PDF_FILE_NAME,
            fileMediaType = PDF_MIME_TYPE,
            vertrouwelijkheidaanduiding = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_VERTROUWELIJK,
            testUser = BEHEERDER_ELK_ZAAKTYPE
        )

        When("""the internal ZAC reindexing endpoint is called for type 'zaak'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/indexeren/herindexeren/ZAAK",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            Then(
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
                        testUser = BEHEERDER_ELK_ZAAKTYPE
                    )
                    JSONObject(response.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                }
            }
        }
        When("""the reindexing endpoint is called for type 'task'""") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/internal/indexeren/herindexeren/TAAK",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            Then(
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
                        testUser = BEHEERDER_ELK_ZAAKTYPE
                    )
                    JSONObject(response.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                }
            }
        }
        When("""the reindexing endpoint is called for type 'document'""") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/internal/indexeren/herindexeren/DOCUMENT",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
                ).toHeaders()
            )
            Then(
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
                        testUser = BEHEERDER_ELK_ZAAKTYPE
                    )
                    JSONObject(response.bodyAsString).getInt("totaal") shouldBeGreaterThan 0
                }
            }
        }
    }
})
