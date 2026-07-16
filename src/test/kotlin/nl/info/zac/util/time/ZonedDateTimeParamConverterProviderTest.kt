/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime

class ZonedDateTimeParamConverterProviderTest : BehaviorSpec({
    val zonedDateTimeParamConverterProvider = ZonedDateTimeParamConverterProvider()

    given("a raw type assignable from ZonedDateTime") {
        `when`("getting a converter") {
            val converter = zonedDateTimeParamConverterProvider.getConverter(
                ZonedDateTime::class.java,
                ZonedDateTime::class.java,
                emptyArray()
            )

            then("it should return a ZonedDateTimeParamConverter") {
                converter.shouldBeInstanceOf<ZonedDateTimeParamConverter>()
            }
        }
    }

    given("a raw type not assignable from ZonedDateTime") {
        `when`("getting a converter") {
            val converter = zonedDateTimeParamConverterProvider.getConverter(
                String::class.java,
                String::class.java,
                emptyArray()
            )

            then("it should return null") {
                converter shouldBe null
            }
        }
    }
})
