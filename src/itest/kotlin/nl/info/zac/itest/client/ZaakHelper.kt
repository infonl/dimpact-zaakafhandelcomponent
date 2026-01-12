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
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestGroup
import okhttp3.Headers
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZaakHelper(
    val zacClient: ZacClient,
) {
    val logger = KotlinLogging.logger {}
    val itestHttpClient = zacClient.itestHttpClient

    /**
     * Creates a new zaak with the given description and zaaktype UUID,
     * then optionally sends a notification to ZAC to index the newly created zaak,
     * and waits until the zaak is findable via the search API using the
     * zaak identification.
     * Because of this we assume that the zaak identification is unique in the context of
     * the integration test suite.
     *
     * @return a Pair of the zaak identification and zaak UUID of the newly created zaak.
     */
    suspend fun createZaak(
        zaakDescription: String = "itestZaakDescription-${System.currentTimeMillis()}",
        zaaktypeUuid: UUID,
        group: TestGroup = BEHANDELAARS_DOMAIN_TEST_1,
        startDate: ZonedDateTime = DATE_TIME_2024_01_01,
        indexZaak: Boolean = false
    ): Pair<String, UUID> {
        var zaakIdentification: String
        var zaakUuid: UUID
        zacClient.createZaak(
            zaakTypeUUID = zaaktypeUuid,
            description = zaakDescription,
            groupId = group.name,
            groupName = group.description,
            startDate = startDate
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakIdentification = getString("identificatie")
                zaakUuid = getString("uuid").run(UUID::fromString)
            }
        }
        if (indexZaak) {
            indexZaak(zaakUuid, zaakIdentification)
        }
        return Pair(zaakIdentification, zaakUuid)
    }

    /**
     * The zaak identification must be unique in the context of the integration test suite,
     * or else zaken indexed by previously run tests may interfere with the indexing check.
     */
    private suspend fun indexZaak(zaakUuid: UUID, zaakIdentification: String) {
        sendZaakCreateNotification(zaakUuid)
        // wait for the indexing to complete by searching for the newly created zaak
        // until we get the expected result
        eventually(30.seconds) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                {
                    "alleenMijnZaken": false,
                    "alleenOpenstaandeZaken": true,
                    "alleenAfgeslotenZaken": false,
                    "alleenMijnTaken": false,
                    "zoeken": { "ZAAK_IDENTIFICATIE": "$zaakIdentification" },
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
    }

    /**
     * Sends a request to the ZAC notification endpoint to notify ZAC about the creation of a zaak,
     * so that ZAC will index the newly created zaak in Solr.
     */
    private fun sendZaakCreateNotification(zaakUuid: UUID) {
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to "zaken",
                    "resource" to "zaak",
                    "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "actie" to "create",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString(),
            addAuthorizationHeader = false
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
    }
}
