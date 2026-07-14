/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class LocalDateConversionsTest : BehaviorSpec({
    afterEach {
        checkUnnecessaryStub()
    }

    given("a LocalDate") {
        val localDate = LocalDate.of(2026, 7, 14)

        `when`("convertToDateTime is called") {
            val zonedDateTime = localDate.convertToDateTime()

            then("the result is start-of-day in the system default time zone") {
                zonedDateTime.toLocalDate() shouldBe localDate
                zonedDateTime.toLocalTime() shouldBe LocalTime.MIDNIGHT
                zonedDateTime.zone shouldBe ZoneId.systemDefault()
            }
        }
    }
})
