/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZaakHelper(
    val zacClient: ZacClient,
) {
    val logger = KotlinLogging.logger {}
    val itestHttpClient = zacClient.itestHttpClient

    suspend fun createAndIndexZaak(
        zaakDescription: String
    ): Pair<String, UUID> {
        var zaak1Identification1: String
        var zaak1Uuid1: UUID
        zacClient.createZaak(
            description = zaakDescription,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2024_01_01,
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaak1Identification1 = getString("identificatie")
                zaak1Uuid1 = getString("uuid").run(UUID::fromString)
            }
        }
        // (re)index all zaken
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/internal/indexeren/herindexeren/ZAAK",
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
            ).toHeaders(),
            addAuthorizationHeader = false
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
        // wait for the indexing to complete by searching for the newly created zaak until we get the expected result
        // note that this assumes that the zaak description is unique
        eventually(5.seconds) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": true,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "ZAAK_OMSCHRIJVING": "$zaakDescription" },
                    "filters": {},
                    "datums": {},
                    "rows": 1,
                    "page": 0,
                    "type": "ZAAK"
                }
            """.trimIndent()
            )
            JSONObject(response.bodyAsString).getInt("totaal") shouldBe 1
        }
        return Pair(zaak1Identification1, zaak1Uuid1)
    }
}
