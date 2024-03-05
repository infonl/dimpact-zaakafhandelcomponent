/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_2_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject

private val zacClient = ZacClient()
private val logger = KotlinLogging.logger {}

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class ZakenRESTServiceTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the create zaak endpoint is called and the user has permissions for the zaaktype used") {
            then("the response should be a 200 HTTP response with the created zaak") {
                zacClient.createZaak(
                    ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID,
                    GROUP_A_ID,
                    GROUP_A_NAME
                ).use { response ->
                    response.isSuccessful shouldBe true
                    JSONObject(response.body!!.string()).apply {
                        getJSONObject("zaaktype").getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                        getJSONObject("zaakdata").apply {
                            getString("zaakUUID") shouldNotBe null
                            getString("zaakIdentificatie") shouldBe ZAAK_2_IDENTIFICATION
                        }
                    }
                }
            }
        }
    }
    given("A zaak has been created") {
        When("the assign group to zaak endpoint is called") {
            then("the group should be assigned to the zaak") {
                // note that this HTTP request currently requires the following environment variable
                // to be set when running this test: JAVA_TOOL_OPTIONS=--add-opens=java.base/java.net=ALL-UNNAMED
                // see: https://github.com/lojewalo/khttp/issues/88
                zacClient.performPatchRequest(
                    url = "${ZAC_API_URI}/zaken/toekennen",
                    requestBodyAsString = "{\n" +
                        "  \"zaakUUID\": \"$zaak1UUID\",\n" +
                        "  \"groepId\": \"$GROUP_A_ID\",\n" +
                        "  \"reden\": \"dummyReason\"\n" +
                        "}"
                ).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true

                    with(responseBody) {
                        shouldContainJsonKeyValue("uuid", zaak1UUID.toString())
                        shouldContainJsonKey("groep")
                        JSONObject(this).getJSONObject("groep").apply {
                            getString("id") shouldBe GROUP_A_ID
                            getString("naam") shouldBe GROUP_A_NAME
                        }
                    }
                }
            }
        }
    }
})
