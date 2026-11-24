/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import nl.info.zac.log.LogCapture
import java.util.logging.Level

class ChannelTest : BehaviorSpec({
    val adapter = Channel.Adapter()

    given("the code 'zaken' of a known channel") {
        `when`("the code is read from JSON") {
            var channel: Channel? = null
            val records = LogCapture(Channel::class).use { logCapture ->
                channel = adapter.adaptFromJson("zaken")
                logCapture.records
            }

            then("it is read as Channel.ZAKEN and nothing is logged") {
                channel shouldBe Channel.ZAKEN
                records.shouldBeEmpty()
            }
        }
    }

    given("an unknown channel code") {
        `when`("the code is read from JSON") {
            var channel: Channel? = Channel.ZAKEN
            val records = LogCapture(Channel::class).use { logCapture ->
                channel = adapter.adaptFromJson("fakeUnknownChannel")
                logCapture.records
            }

            then("it is read as null and a warning with the unknown code is logged") {
                channel shouldBe null
                with(records.single()) {
                    level shouldBe Level.WARNING
                    message shouldBe "Unknown channel"
                    mdc["code"] shouldBe "fakeUnknownChannel"
                }
            }
        }
    }
})
