/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.time.UtcDefaultTimeZone
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LocalDateConversionsTest : BehaviorSpec({
    extension(UtcDefaultTimeZone())

    given("the JVM time zone UTC and the LocalDate 30-07-2026") {
        val localDate = LocalDate.of(2026, 7, 30)

        `when`("toZonedDateTime is called") {
            val zonedDateTime = localDate.toZonedDateTime()

            then("the result is the start of 30-07-2026 in Europe/Amsterdam") {
                zonedDateTime shouldBe ZonedDateTime.of(2026, 7, 30, 0, 0, 0, 0, ZoneId.of("Europe/Amsterdam"))
            }
        }
    }
})
