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
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.net.HttpURLConnection.HTTP_OK

@Order(TEST_SPEC_ORDER_AFTER_REFERENCE_TABLES_UPDATED)
class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val bpmnZaakType = """
        {
            "id": 1,
            "zaaktypeUuid": "$ZAAKTYPE_BPMN_TEST_UUID",
            "zaaktypeOmschrijving": "$ZAAKTYPE_BPMN_TEST_DESCRIPTION",
            "bpmnProcessDefinitionKey": "$BPMN_TEST_PROCESS_DEFINITION_KEY",
            "productaanvraagtype": "$ZAAKTYPE_BPMN_PRODUCTAANVRAAG_TYPE",
            "groepNaam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
        }
    """.trimIndent()

    Given("A BPMN zaaktype configuration was created in the overall test setup") {
        lateinit var responseBody: String

        When("the BPMN zaaktype configuration is retrieved") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/zaaktype-bpmn-configuration/$BPMN_TEST_PROCESS_DEFINITION_KEY"
            )

            Then("the response is successful") {
                responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }

            And("the expected zaak type data is returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields bpmnZaakType
            }
        }

        When("list of all BPMN zaaktype configurations is retrieved") {
            lateinit var responseBody: String

            val response = itestHttpClient.performGetRequest("$ZAC_API_URI/zaaktype-bpmn-configuration")

            Then("the response is successful") {
                responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
            }

            And("the expected zaak type data list is returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields "[$bpmnZaakType]"
            }
        }
    }
})
