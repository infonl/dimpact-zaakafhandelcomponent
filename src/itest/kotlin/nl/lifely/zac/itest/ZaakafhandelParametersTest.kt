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
import io.kotest.provided.ZAAKTYPE_PRODUCTAANVRAAG_UUID

private val logger = KotlinLogging.logger {}

class ZaakafhandelParametersTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list zaakafhandelparameterts endpoint is called for our zaaktype under test") {
            then("the response should be ok and it should return the zaakafhandelparameters") {
                khttp.get(
                    url = "${ProjectConfig.zacContainer.managementUrl}/zaakafhandelParameters/$ZAAKTYPE_PRODUCTAANVRAAG_UUID"
                ).apply {
                    logger.info { "Zaakafhandelparameters response: $text" }
                    statusCode shouldBe HttpStatus.SC_OK
                }
            }
        }
    }
})
