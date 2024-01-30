/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.KeycloakClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.SMARTDOCUMENTS_MOCK_BASE_URI
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(1)
class InformatieObjectenTest : BehaviorSpec() {
    init {
        given(
            "ZAC and all related Docker containers are running and zaak exists"
        ) {
            When("the create document informatie objecten endpoint is called") {
                then(
                    "the 'unattended document creation wizard' is started in Smartdocuments"
                ) {
                    logger.info { "Calling documentcreatie endpoint for zaak with UUID: '$zaakUUID' to create document in Smartdocuments" }
                    khttp.post(
                        url = "${ItestConfiguration.ZAC_API_URI}/informatieobjecten/documentcreatie",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
                        ),
                        data = JSONObject(
                            mapOf(
                                "zaakUUID" to zaakUUID
                            )
                        )
                    ).apply {
                        logger.info { "/informatieobjecten/documentcreatie response: $text" }

                        statusCode shouldBe HttpStatus.SC_OK
                        JSONObject(text).getString("redirectURL") shouldBe
                            "$SMARTDOCUMENTS_MOCK_BASE_URI/smartdocuments/wizard?ticket=dummySmartdocumentsTicketID"
                    }
                }
            }
        }
    }
}
