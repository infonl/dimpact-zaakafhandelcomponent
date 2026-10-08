/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_5_UUID
import nl.info.zac.itest.util.queryZacDatabase
import java.net.HttpURLConnection.HTTP_OK

private const val WARNING_WINDOW_COLUMN = "einddatum_gepland_waarschuwing"

class ZaaktypeBpmnConfigurationPreservedSettingsTest : BehaviorSpec({
    val zacClient = ZacClient(ItestHttpClient())
    val bpmnConfigurationCondition = "zaaktype_uuid = '$ZAAKTYPE_BPMN_TEST_5_UUID'"

    afterSpec {
        queryZacDatabase(
            "UPDATE zaakafhandelcomponent.zaaktype_configuration SET $WARNING_WINDOW_COLUMN = NULL " +
                "WHERE $bpmnConfigurationCondition"
        )
    }

    given("a BPMN zaaktype configuration with an einddatum-gepland warning window of 3 days") {
        queryZacDatabase(
            "UPDATE zaakafhandelcomponent.zaaktype_configuration SET $WARNING_WINDOW_COLUMN = 3 " +
                "WHERE $bpmnConfigurationCondition"
        )

        `when`("a beheerder stores the configuration through the BPMN REST resource, whose payload has no warning window") {
            val response = zacClient.createZaaktypeBpmnConfiguration(
                zaakTypeUuid = ZAAKTYPE_BPMN_TEST_5_UUID,
                zaakTypeDescription = ZAAKTYPE_BPMN_TEST_5_DESCRIPTION,
                bpmnProcessDefinitionKey = BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY,
                productaanvraagType = ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE,
                defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
                defaultBehandelaarId = BEHANDELAAR_1.username,
                nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID,
                testUser = BEHEERDER_1
            )

            then("the configuration is stored and keeps its warning window of 3 days") {
                response.code shouldBe HTTP_OK
                queryZacDatabase(
                    "SELECT $WARNING_WINDOW_COLUMN FROM zaakafhandelcomponent.zaaktype_configuration " +
                        "WHERE $bpmnConfigurationCondition"
                ) shouldBe listOf("3")
            }
        }
    }
})
