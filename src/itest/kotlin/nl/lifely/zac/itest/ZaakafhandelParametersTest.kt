/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

private val logger = KotlinLogging.logger {}
private val itestHttpClient = ItestHttpClient()

class ZaakafhandelParametersTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list zaakafhandelparameterts endpoint is called for our zaaktype under test") {
            then("the response should be ok and it should return the zaakafhandelparameters") {
                itestHttpClient.performGetRequest(
                    url = "${ZAC_API_URI}/zaakafhandelParameters/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
                ).use { response ->
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                    with(responseBody) {
                        shouldContainJsonKeyValue(
                            "zaaktype.identificatie",
                            ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                        )
                    }
                }
            }
        }
    }
})
