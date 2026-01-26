/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.bpmn

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import java.net.HttpURLConnection

class ZaaktypeBpmnConfigurationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val bpmnZaakType1 = """
        {
            "id": 1,
            "zaaktypeUuid": "${ZAAKTYPE_BPMN_TEST_1_UUID}",
            "zaaktypeOmschrijving": "${ZAAKTYPE_BPMN_TEST_1_DESCRIPTION}",
            "bpmnProcessDefinitionKey": "${BPMN_TEST_PROCESS_DEFINITION_KEY}",
            "productaanvraagtype": "${ZAAKTYPE_BPMN_TEST_1_PRODUCTAANVRAAG_TYPE}",
            "groepNaam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
        }
    """.trimIndent()
    val bpmnZaakType2 = """
        {
            "id": 2,
            "zaaktypeUuid": "${ZAAKTYPE_BPMN_TEST_2_UUID}",
            "zaaktypeOmschrijving": "${ZAAKTYPE_BPMN_TEST_2_DESCRIPTION}",
            "bpmnProcessDefinitionKey": "${BPMN_TEST_USER_MANAGEMENT_PROCESS_DEFINITION_KEY}",
            "productaanvraagtype": "${ZAAKTYPE_BPMN_TEST_2_PRODUCTAANVRAAG_TYPE}",
            "groepNaam": "${BEHANDELAARS_DOMAIN_TEST_1.description}"
        }
    """.trimIndent()

    Given("A BPMN zaaktype configuration was created in the overall test setup") {
        lateinit var responseBody: String

        When("the BPMN zaaktype configuration is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "${ZAC_API_URI}/zaaktype-bpmn-configuration/${BPMN_TEST_PROCESS_DEFINITION_KEY}",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the response is successful") {
                responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpURLConnection.HTTP_OK
            }

            And("the expected zaak type data is returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields bpmnZaakType1
            }
        }

        When("list of all BPMN zaaktype configurations is retrieved") {
            lateinit var responseBody: String

            val response = itestHttpClient.performGetRequest(
                url = "${ZAC_API_URI}/zaaktype-bpmn-configuration",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the response is successful") {
                responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HttpURLConnection.HTTP_OK
            }

            And("the expected zaak type data list is returned") {
                responseBody shouldEqualJsonIgnoringExtraneousFields "[$bpmnZaakType1, $bpmnZaakType2]"
            }
        }
    }
})
