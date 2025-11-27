/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestGroup
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class TaskHelper(
    val zacClient: ZacClient,
) {
    val itestHttpClient = zacClient.itestHttpClient
    val logger = KotlinLogging.logger {}

    suspend fun startAanvullendeInformatieTaskForZaak(
        zaakUuid: UUID,
        zaakIdentificatie: String,
        fatalDate: LocalDate,
        group: TestGroup
    ) {
        val response = zacClient.startAanvullendeInformatieTaskForZaak(
            zaakUUID = zaakUuid,
            fatalDate = fatalDate,
            group = group
        )
        response.code shouldBe HTTP_NO_CONTENT
        // The task is automatically indexed, so no need to (re)index here.
        // However, the indexing may still take some time to complete, so we perform a search
        // here to ensure the task is findable.
        eventually(10.seconds) {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                       {
                        "alleenMijnZaken": false,
                        "alleenOpenstaandeZaken": false,
                        "alleenAfgeslotenZaken": false,
                        "alleenMijnTaken": false,
                        "zoeken": { "TAAK_ZAAK_ID": "$zaakIdentificatie" },
                        "filters": { "TAAK_NAAM": { "values": [ "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM" ] } },
                        "datums": {},
                        "rows": 10,
                        "page": 0,
                        "type": "TAAK"
                        }
                """.trimIndent()
            )
            JSONObject(response.bodyAsString).getInt("totaal") shouldBe 1
        }
    }
}
