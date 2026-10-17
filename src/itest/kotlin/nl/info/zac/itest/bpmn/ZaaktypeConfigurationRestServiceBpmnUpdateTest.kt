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
private const val PRESERVED_TEMPLATE_NAME = "fakePreservedConfirmationTemplate"

class ZaaktypeConfigurationRestServiceBpmnUpdateTest : BehaviorSpec({
    val zacClient = ZacClient(ItestHttpClient())
    val bpmnConfigurationId =
        "SELECT id FROM zaakafhandelcomponent.zaaktype_configuration WHERE zaaktype_uuid = '$ZAAKTYPE_BPMN_TEST_5_UUID'"

    fun storeBpmnTest5Configuration(einddatumGeplandWarningWindow: Int) = zacClient.createZaaktypeBpmnConfiguration(
        zaakTypeUuid = ZAAKTYPE_BPMN_TEST_5_UUID,
        zaakTypeDescription = ZAAKTYPE_BPMN_TEST_5_DESCRIPTION,
        bpmnProcessDefinitionKey = BPMN_PERMISSION_CHECK_PROCESS_DEFINITION_KEY,
        productaanvraagType = ZAAKTYPE_BPMN_TEST_5_PRODUCTAANVRAAG_TYPE,
        defaultGroupName = GROUP_BEHANDELAARS_TEST_1.name,
        defaultBehandelaarId = BEHANDELAAR_1.username,
        nietOntvankelijkResultaattype = ZAAKTYPE_BPMN_TEST_5_RESULTAATTYPE_AFGEBROKEN_UUID,
        einddatumGeplandWarningWindow = einddatumGeplandWarningWindow,
        testUser = BEHEERDER_1
    )

    afterSpec {
        queryZacDatabase(
            "DELETE FROM zaakafhandelcomponent.zaaktype_email_parameters " +
                "WHERE template_name = '$PRESERVED_TEMPLATE_NAME'"
        )
        queryZacDatabase(
            "UPDATE zaakafhandelcomponent.zaaktype_configuration SET $WARNING_WINDOW_COLUMN = NULL " +
                "WHERE id IN ($bpmnConfigurationId)"
        )
    }

    given(
        "a BPMN zaaktype configuration with confirmation email parameters and an einddatum-gepland warning window of 3 days"
    ) {
        queryZacDatabase(
            """
            INSERT INTO zaakafhandelcomponent.zaaktype_email_parameters
                (id, zaaktype_configuration_id, enabled, template_name, email_sender)
            SELECT nextval('zaakafhandelcomponent.sq_zaaktype_email_parameters'), id, true,
                '$PRESERVED_TEMPLATE_NAME', 'GEMEENTE'
            FROM zaakafhandelcomponent.zaaktype_configuration
            WHERE id IN ($bpmnConfigurationId)
            """.trimIndent()
        )
        queryZacDatabase(
            "UPDATE zaakafhandelcomponent.zaaktype_configuration SET $WARNING_WINDOW_COLUMN = 3 " +
                "WHERE id IN ($bpmnConfigurationId)"
        )

        `when`("a beheerder stores it with an einddatum-gepland warning window of 5 days") {
            val response = storeBpmnTest5Configuration(einddatumGeplandWarningWindow = 5)

            then("the configuration is stored with a warning window of 5 days") {
                response.code shouldBe HTTP_OK
                queryZacDatabase(
                    "SELECT $WARNING_WINDOW_COLUMN FROM zaakafhandelcomponent.zaaktype_configuration " +
                        "WHERE id IN ($bpmnConfigurationId)"
                ) shouldBe listOf("5")
            }

            and("it keeps its confirmation email parameters, which the payload of a BPMN configuration does not carry") {
                queryZacDatabase(
                    "SELECT template_name FROM zaakafhandelcomponent.zaaktype_email_parameters " +
                        "WHERE zaaktype_configuration_id IN ($bpmnConfigurationId)"
                ) shouldBe listOf(PRESERVED_TEMPLATE_NAME)
            }
        }
    }
})
