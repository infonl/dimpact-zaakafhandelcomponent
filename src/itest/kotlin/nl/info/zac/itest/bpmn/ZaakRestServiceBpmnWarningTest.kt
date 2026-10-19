/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.ZaakHelper
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_BPMN_WAARSCHUWING_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZonedDateTime

class ZaakRestServiceBpmnWarningTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val zaakHelper = ZaakHelper(zacClient)

    fun storeBpmnTest1Configuration(fatalDateWarningWindow: Int?) = zacClient.createZaaktypeBpmnConfiguration(
        zaakTypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
        zaakTypeDescription = ZAAKTYPE_BPMN_TEST_1_DESCRIPTION,
        bpmnProcessDefinitionKey = BPMN_TEST_PROCESS_DEFINITION_KEY,
        productaanvraagType = ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE,
        defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
        defaultBehandelaarId = BEHANDELAAR_1.username,
        nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_1_RESULTAATTYPE_AFGEBROKEN_UUID,
        fatalDateWarningWindow = fatalDateWarningWindow,
        testUser = BEHEERDER_1
    )

    afterSpec {
        storeBpmnTest1Configuration(fatalDateWarningWindow = null).code shouldBe HTTP_OK
    }

    given(
        """
        a BPMN zaaktype with a fatal date warning window of 1 day, and a zaak of that zaaktype that is assigned to
        myself and whose fatal date is tomorrow
        """
    ) {
        storeBpmnTest1Configuration(fatalDateWarningWindow = 1).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
        }
        val (zaakIdentification, zaakUuid) = zaakHelper.createZaak(
            zaaktypeUuid = ZAAKTYPE_BPMN_TEST_1_UUID,
            startDate = ZonedDateTime.now().minusDays(1),
            group = GROUP_BEHANDELAARS_TEST_1,
            testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1,
            behandelaarId = BEHANDELAAR_BPMN_WAARSCHUWING_1.username,
            behandelaarName = BEHANDELAAR_BPMN_WAARSCHUWING_1.displayName
        )
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid",
            requestBodyAsString = """
                {
                    "zaak": {
                        "uiterlijkeEinddatumAfdoening": "${ZonedDateTime.now().plusDays(1)}"
                    },
                    "reden": "fakeReason"
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1
        ).code shouldBe HTTP_OK

        `when`("I list my zaak warnings") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/zaken/waarschuwing",
                testUser = BEHANDELAAR_BPMN_WAARSCHUWING_1
            )

            then("the BPMN zaak is listed, because the warning window of its zaaktype applies to BPMN zaken too") {
                response.code shouldBe HTTP_OK
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                JSONArray(responseBody).let { zaken ->
                    zaken.length() shouldBe 1
                    zaken.getJSONObject(0).getString("identificatie") shouldBe zaakIdentification
                }
            }
        }
    }
})
