/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

class LocalDateParamConverterProviderTest : BehaviorSpec({
    val localDateParamConverterProvider = LocalDateParamConverterProvider()

    given("a parameter of the type LocalDate") {
        `when`("a converter is requested") {
            val converter = localDateParamConverterProvider.getConverter(
                LocalDate::class.java,
                LocalDate::class.java,
                emptyArray()
            )

            then("the LocalDate converter is returned") {
                converter.shouldBeInstanceOf<LocalDateParamConverter>()
            }
        }
    }

    given("a parameter of another type") {
        `when`("a converter is requested") {
            val converter = localDateParamConverterProvider.getConverter(
                String::class.java,
                String::class.java,
                emptyArray()
            )

            then("no converter is returned") {
                converter shouldBe null
            }
        }
    }
})
