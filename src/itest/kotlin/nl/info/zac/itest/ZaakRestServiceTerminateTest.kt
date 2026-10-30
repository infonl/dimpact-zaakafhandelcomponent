/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ZaakRestServiceTerminateTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val taskHelper = TaskHelper(zacClient)

    given("a CMMN zaak with an open taak that is assigned to the behandelaar and found by the taken search") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentificatie: String
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                zaakUuid = getString("uuid").let(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentificatie,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            waitForTaskToBeIndexed = true,
            medewerker = BEHANDELAAR_1,
            testUser = BEHANDELAAR_1
        )

        `when`("the zaak is terminated") {
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/afbreken",
                requestBodyAsString = """{ "zaakbeeindigRedenId": "ZAAK_NIET_ONTVANKELIJK" }""",
                testUser = BEHANDELAAR_1
            )

            then("the zaak is closed") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldContainJsonKeyValue("isOpen", false)
            }

            and("its ended taak is no longer found by the taken search") {
                eventually(10.seconds) {
                    zacClient.searchForTasks(
                        zaakIdentificatie = zaakIdentificatie,
                        taskName = HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM,
                        testUser = BEHANDELAAR_1
                    ).let(::JSONObject).getInt("totaal") shouldBe 0
                }
            }
        }
    }
})
