/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI

@Order(TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED)
class ZaaktypeBpmnProcessDefinitionRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("No existing zaaktype - BPMN process definition mapping") {
        When("a mapping is created") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/zaaktype-bpmn-process-definition/$BPMN_TEST_PROCESS_ID",
                requestBodyAsString = """{ 
                  "zaaktypeUuid": "$ZAAKTYPE_BPMN_TEST_UUID",
                  "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
                  "productaanvraagtype": "$ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE",
                  "groepNaam": "$TEST_GROUP_A_DESCRIPTION"
                }
                """.trimIndent()
            )

            Then("the response is successful") {
                val responseBody = response.body.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
            }
        }
    }
})
