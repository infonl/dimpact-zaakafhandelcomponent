/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec

private val logger = KotlinLogging.logger {}

class AppContainerTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the build information endpoint is called") {
            then("the response should be ok") {
                // test code
                logger.info { "test" }
            }
        }
    }
})
