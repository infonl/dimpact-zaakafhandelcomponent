/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
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

class TaskRestServiceCompleteTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val taskHelper = TaskHelper(zacClient)

    Given(
        """A zaak has been created,
            two 'Aanvullende informatie' tasks have been started,
            and a behandelaar is logged in"""
    ) {
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
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid.let(UUID::fromString),
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = BEHANDELAARS_DOMAIN_TEST_1,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )
        lateinit var taskArray: JSONArray

        When("the get tasks for a zaak endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/zaak/$zaakUuid",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the list with tasks for this zaak is returned") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                taskArray = JSONArray(responseBody)
            }
        }

        When("first 'Aanvullende informatie' task is completed") {
            val taskObject = taskArray.getJSONObject(0)
            taskObject.put("toelichting", "completed")
            taskObject.put("status", "AFGEROND")
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = taskObject.toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the taken toelichting and status are updated") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldBeJsonObject()
                responseBody.shouldContainJsonKeyValue("toelichting", "completed")
                responseBody.shouldContainJsonKeyValue("status", "AFGEROND")
            }

            And("the zaak status remains in `aanvullende informatie`") {
                val response = zacClient.retrieveZaak(zaakIdentification, BEHANDELAAR_DOMAIN_TEST_1)
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("$.status.naam", "Wacht op aanvullende informatie")
            }
        }
    }
})
