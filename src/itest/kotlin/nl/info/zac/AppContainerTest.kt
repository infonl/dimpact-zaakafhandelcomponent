/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac

import io.kotest.core.spec.style.BehaviorSpec

class AppContainerTest : BehaviorSpec({
    given("ZAC Docker container and all related Docker containers are running") {
        When("the build information endpoint is called") {
            then("the response should be ok") {
                // test code
                //"health-check/build-informatie"

            }
        }
    }
})
