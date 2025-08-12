/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.util.UUID

/**
 * This test creates a zaak, adds a task to complete the intake phase, then adds a document, starts the 'Goedkeuren' task
 * and completes this task by approving the document.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
@Suppress("MagicNumber")
class TaskRestServiceGoedkeurenTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    val logger = KotlinLogging.logger {}

    Given("A zaak has been created that has finished the intake phase with the status 'admissible'") {
        lateinit var zaakUUID: UUID
        val intakeId: Int
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2000_01_01
        ).run {
            JSONObject(body.string()).run {
                getJSONObject("zaakdata").run {
                    zaakUUID = getString("zaakUUID").run(UUID::fromString)
                }
            }
        }
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUUID/userEventListenerPlanItems"
        ).run {
            JSONArray(body.string()).getJSONObject(0).run {
                intakeId = getString("id").toInt()
            }
        }
        // wait for OpenZaak to accept this request
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
            {
                "zaakUuid":"$zaakUUID",
                "planItemInstanceId":"$intakeId",
                "actie":"$ACTIE_INTAKE_AFRONDEN",
                "zaakOntvankelijk":true
            }
            """.trimIndent()
        ).run {
            logger.info { "Response: ${body.string()}" }
            code shouldBe HTTP_NO_CONTENT
        }
        // TODO: add a document
        // TODO: start the 'Goedkeuren' task
        // TODO: complete the 'Goedkeuren' task by approving the document
    }
})
