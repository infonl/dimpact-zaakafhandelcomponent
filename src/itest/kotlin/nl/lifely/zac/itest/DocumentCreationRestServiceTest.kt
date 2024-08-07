/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.SMART_DOCUMENTS_MOCK_BASE_URI
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import org.json.JSONObject

/**
 * This test assumes a zaak has been created, and a task has been started in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_RETRIEVED)
class DocumentCreationRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given(
        "ZAC and all related Docker containers are running and zaak exists"
    ) {
        When("the create document informatie objecten endpoint is called") {
            val endpointUrl = "$ZAC_API_URI/documentcreation/createdocumentattended"
            logger.info { "Calling $endpointUrl endpoint" }
            val response = itestHttpClient.performJSONPostRequest(
                url = endpointUrl,
                requestBodyAsString = JSONObject(
                    mapOf(
                        "zaakUUID" to zaakProductaanvraag1Uuid
                    )
                ).toString()
            )
            Then(
                "the response should be OK and the response should contain a redirect URL to Smartdocuments"
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_OK

                with(responseBody) {
                    shouldContainJsonKeyValue(
                        "redirectURL",
                        "$SMART_DOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=dummySmartdocumentsTicketID"
                    )
                }
            }
        }
    }
})
