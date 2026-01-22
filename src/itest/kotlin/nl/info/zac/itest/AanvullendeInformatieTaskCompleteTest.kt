/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID

class AanvullendeInformatieTaskCompleteTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val taskHelper = TaskHelper(zacClient)

    Given("A zaak with one 'Aanvullende informatie' task and a logged-in behandelaar") {
        lateinit var zaakUuid: String
        lateinit var zaakIdentification: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid")
                zaakIdentification = getString("identificatie")
            }
        }
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid.let(UUID::fromString),
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = BEHANDELAARS_DOMAIN_TEST_1,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )
        val tasksResponse = itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/taken/zaak/$zaakUuid",
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )
        val responseBody = tasksResponse.bodyAsString
        logger.info { "Response: $responseBody" }
        tasksResponse.code shouldBe HTTP_OK
        val taskArray = JSONArray(responseBody)

        When("the (last) 'Aanvullende informatie' task is closed") {
            val taskObject = taskArray.getJSONObject(0)
            taskObject.put("toelichting", "completed")
            taskObject.put("status", "AFGEROND")
            val completeTaskResponse = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = taskObject.toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the taken toelichting and status are updated") {
                val responseBody = completeTaskResponse.bodyAsString
                logger.info { "Response: $responseBody" }
                completeTaskResponse.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("toelichting", "completed")
                responseBody.shouldContainJsonKeyValue("status", "AFGEROND")
            }

            And("the zaak status is set back to `Intake`") {
                val response = zacClient.retrieveZaak(
                    id = zaakIdentification,
                    testUser = BEHANDELAAR_DOMAIN_TEST_1
                )
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("$.status.naam", "Intake")
            }
        }
    }
})
