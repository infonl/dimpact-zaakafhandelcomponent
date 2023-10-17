/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private val logger = KotlinLogging.logger {}

class AppContainerTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the build information endpoint is called") {
            then("the response should be ok") {
                khttp.get("${ZACContainer.API_URL}/health-check/build-informatie").apply {
                    logger.info { "response: $this" }
                    statusCode shouldBe HttpStatus.SC_OK
                }
            }
        }
    }
})
