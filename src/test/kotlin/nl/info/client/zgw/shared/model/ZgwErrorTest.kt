/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ZgwErrorTest : BehaviorSpec({
    given("a ZGW error") {
        val zgwError = ZgwError(
            type = null,
            code = "fakeCode",
            title = "fakeTitle",
            status = 404,
            detail = "fakeDetail",
            instance = null
        )

        `when`("toString is called") {
            val result = zgwError.toString()

            then("it should format the status, title and detail") {
                result shouldBe "(404) Title: fakeTitle, Detail: fakeDetail"
            }
        }
    }
})
