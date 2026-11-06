/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_1_EN_BRP_ZOEKER_2
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.GREENMAIL_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Isolate
class TaskRestServiceTaakAfgerondSignaleringTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val taskHelper = TaskHelper(zacClient)
    val afterThirtySeconds = eventuallyConfig {
        duration = 30.seconds
        interval = 500.milliseconds
    }

    suspend fun shouldHaveReceivedTaakAfgerondMail(zaakIdentificatie: String) =
        eventually(afterThirtySeconds) {
            val response = itestHttpClient.performGetRequest(
                url = "$GREENMAIL_API_URI/user/${BEHANDELAAR_1_EN_BRP_ZOEKER_2.email}/messages/"
            )
            response.code shouldBe HTTP_OK
            JSONArray(response.bodyAsString).let { mails ->
                (0 until mails.length())
                    .map(mails::getJSONObject)
                    .filter { it.getString("subject") == "Een taak voor zaak $zaakIdentificatie is afgerond" }
            } shouldHaveSize 1
        }

    given("A CMMN zaak of another behandelaar with an open task") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentificatie: String
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            behandelaarId = BEHANDELAAR_1_EN_BRP_ZOEKER_2.username,
            behandelaarName = BEHANDELAAR_1_EN_BRP_ZOEKER_2.displayName,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid,
            zaakIdentificatie = zaakIdentificatie,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_1
        )

        `when`("a behandelaar who is not the behandelaar of the zaak completes the task") {
            val task = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/zaak/$zaakUuid",
                testUser = BEHANDELAAR_1
            ).run {
                code shouldBe HTTP_OK
                JSONArray(bodyAsString).getJSONObject(0)
            }
            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = task.put("toelichting", "fakeToelichting").toString(),
                testUser = BEHANDELAAR_1
            )

            then("the behandelaar of the zaak is mailed that the task has been completed") {
                logger.info { "Response: ${response.bodyAsString}" }
                response.code shouldBe HTTP_OK
                shouldHaveReceivedTaakAfgerondMail(zaakIdentificatie)
            }
        }
    }

    given("A BPMN zaak of another behandelaar with an open task") {
        lateinit var zaakUuid: UUID
        lateinit var zaakIdentificatie: String
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_BPMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            behandelaarId = BEHANDELAAR_1_EN_BRP_ZOEKER_2.username,
            behandelaarName = BEHANDELAAR_1_EN_BRP_ZOEKER_2.displayName,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).getJSONObject("zaakdata").run {
                zaakUuid = getString("zaakUUID").run(UUID::fromString)
                zaakIdentificatie = getString("zaakIdentificatie")
            }
        }
        eventually(afterThirtySeconds) {
            zacClient.searchForTasks(
                zaakIdentificatie = zaakIdentificatie,
                taskName = BPMN_USER_MANAGEMENT_DEFAULT_TASK_NAME,
                testUser = BEHANDELAAR_1
            ).let(::JSONObject).getInt("totaal") shouldBe 1
        }

        `when`("a behandelaar who is not the behandelaar of the zaak completes the task") {
            zacClient.submitFormData(bpmnZaakUuid = zaakUuid, taakData = "{}", testUser = BEHANDELAAR_1)

            then("the behandelaar of the zaak is mailed that the task has been completed") {
                shouldHaveReceivedTaakAfgerondMail(zaakIdentificatie)
            }
        }
    }
})
