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

class ResourceTest : BehaviorSpec({
    val adapter = Resource.Adapter()

    given("the code 'zaak' of a known resource") {
        `when`("the code is read from JSON") {
            var resource: Resource? = null
            val records = LogCapture(Resource::class).use { logCapture ->
                resource = adapter.adaptFromJson("zaak")
                logCapture.records
            }

            then("it is read as Resource.ZAAK and nothing is logged") {
                resource shouldBe Resource.ZAAK
                records.shouldBeEmpty()
            }
        }
    }

    given("an unknown resource code") {
        `when`("the code is read from JSON") {
            var resource: Resource? = Resource.ZAAK
            val records = LogCapture(Resource::class).use { logCapture ->
                resource = adapter.adaptFromJson("fakeUnknownResource")
                logCapture.records
            }

            then("it is read as null and a warning with the unknown code is logged") {
                resource shouldBe null
                with(records.single()) {
                    level shouldBe Level.WARNING
                    message shouldBe "Unknown resource"
                    mdc["code"] shouldBe "fakeUnknownResource"
                }
            }
        }
    }
})
