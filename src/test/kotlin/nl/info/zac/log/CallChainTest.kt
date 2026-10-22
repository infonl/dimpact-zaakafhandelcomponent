/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

class CallChainTest : BehaviorSpec({

    given("ZAC code that calls into a class that is not on the call stack") {
        `when`("the ZAC call chain is determined") {
            val callChain = zacCallChain(String::class)

            then("the chain starts with the calling ZAC class and does not contain the call chain function itself") {
                callChain shouldStartWith "CallChainTest"
                callChain shouldNotContain "CallChainKt"
            }
        }
    }

    given("ZAC code that is part of the called class and all of its inner classes") {
        `when`("the ZAC call chain is determined") {
            val callChain = zacCallChain(CallChainTest::class)

            then("the frames of the called class are left out, so that only callers outside of it remain") {
                callChain shouldBe ""
            }
        }
    }
})
