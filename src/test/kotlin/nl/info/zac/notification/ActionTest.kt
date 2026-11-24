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

class ActionTest : BehaviorSpec({
    val adapter = Action.Adapter()

    given("the code 'partial_update' of a known action") {
        `when`("the code is read from JSON") {
            var action: Action? = null
            val records = LogCapture(Action::class).use { logCapture ->
                action = adapter.adaptFromJson("partial_update")
                logCapture.records
            }

            then("it is read as Action.UPDATE and nothing is logged") {
                action shouldBe Action.UPDATE
                records.shouldBeEmpty()
            }
        }
    }

    given("an unknown action code") {
        `when`("the code is read from JSON") {
            var action: Action? = Action.UPDATE
            val records = LogCapture(Action::class).use { logCapture ->
                action = adapter.adaptFromJson("fakeUnknownAction")
                logCapture.records
            }

            then("it is read as null and a warning with the unknown code is logged") {
                action shouldBe null
                with(records.single()) {
                    level shouldBe Level.WARNING
                    message shouldBe "Unknown action"
                    mdc["code"] shouldBe "fakeUnknownAction"
                }
            }
        }
    }
})
