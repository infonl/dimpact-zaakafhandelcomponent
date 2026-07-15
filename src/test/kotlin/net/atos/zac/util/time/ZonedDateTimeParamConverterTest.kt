/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTimeParamConverterTest : BehaviorSpec({
    val zoneId = ZoneId.of("+02:00")

    given("ZonedDateTime date") {
        val zonedDateTime = ZonedDateTime.of(2024, 10, 7, 0, 0, 0, 0, zoneId)

        `when`("converted to string") {
            val zonedDateTimeString = ZonedDateTimeParamConverter().toString(zonedDateTime)

            then("it should convert correctly") {
                zonedDateTimeString shouldBe "2024-10-07T00:00:00+02:00"
            }
        }
    }

    given("Date as string in ISO_OFFSET_DATE_TIME format") {
        val zonedDateTimeString = "2024-10-07T00:00:00+02:00"

        `when`("converted to ZonedDateTime") {
            val zonedDateTime = ZonedDateTimeParamConverter().fromString(zonedDateTimeString)

            then("it should convert correctly") {
                zonedDateTime shouldBe ZonedDateTime.of(2024, 10, 7, 0, 0, 0, 0, zoneId)
            }
        }
    }
})
