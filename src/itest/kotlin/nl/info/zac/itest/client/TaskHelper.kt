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
import nl.info.zac.itest.config.TestUser
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class TaskHelper(
    val zacClient: ZacClient,
) {
    val itestHttpClient = zacClient.itestHttpClient
    val logger = KotlinLogging.logger {}

    /**
     * Starts an 'Aanvullende informatie' human task for the given zaak UUID,
     * and then optionally performs a search to ensure the task is indexed and findable,
     * and finally retrieves and returns the ID of the created task.
     *
     * @param zaakUuid the UUID of the zaak for which to start the task.
     * @param zaakIdentificatie the identification of the zaak for search purposes.
     * @param fatalDate the fatal date for the task.
     * @param group the group under which to start the task.
     * @param waitForTaskToBeIndexed whether to wait until the created task is indexed and findable before retrieving its ID.
     * @param testUser the user performing the operation.
     * @return the ID of the created 'Aanvullende informatie' task.
     */
    @Suppress("LongParameterList")
    suspend fun startAanvullendeInformatieTaskForZaak(
        zaakUuid: UUID,
        zaakIdentificatie: String,
        fatalDate: LocalDate,
        group: TestGroup,
        waitForTaskToBeIndexed: Boolean = false,
        testUser: TestUser
    ): String {
        val response = zacClient.startAanvullendeInformatieTaskForZaak(
            zaakUUID = zaakUuid,
            fatalDate = fatalDate,
            group = group,
            testUser = testUser
        )
        response.code shouldBe HTTP_NO_CONTENT
        if (waitForTaskToBeIndexed) {
            waitForTaskToBeIndexed(zaakIdentificatie)
        }
        val getTaskResponse = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/taken/zaak/$zaakUuid"
        )
        val responseBody = getTaskResponse.bodyAsString
        logger.info { "Response: $responseBody" }
        getTaskResponse.code shouldBe HTTP_OK
        val taskCount = JSONArray(responseBody).length()
        logger.info { "Number of tasks for zaak with UUID '$zaakUuid': $taskCount" }
        // should there be multiple tasks for this zaak, we return the ID of the last one we just created
        return JSONArray(responseBody).getJSONObject(taskCount - 1).getString("id")
    }

    private suspend fun waitForTaskToBeIndexed(zaakIdentificatie: String) {
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
