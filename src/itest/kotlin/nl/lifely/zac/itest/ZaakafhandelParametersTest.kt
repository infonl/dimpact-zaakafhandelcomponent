/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.provided.ProjectConfig
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class ZaakafhandelParametersTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list zaakafhandelparameterts endpoint is called for our zaaktype under test") {
            then("the response should be ok and it should return the zaakafhandelparameters") {
                khttp.get(
                    url = "${ZAC_API_URI}/zaakafhandelParameters/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                    headers = mapOf("Authorization" to "Bearer ${ProjectConfig.keycloakClient.requestAccessToken()}")
                ).apply {
                    logger.info { "Zaakafhandelparameters response: $text" }
                    val zaakafhandelparameters = JSONObject(text)
                    zaakafhandelparameters
                        .getJSONObject("zaaktype")
                        .getString("identificatie") shouldBe ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
                    statusCode shouldBe HttpStatus.SC_OK
                }
            }
        }
    }
})
