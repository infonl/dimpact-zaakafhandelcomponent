/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.shared.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SorteerRichtingTest : BehaviorSpec({

    listOf(
        "asc" to SorteerRichting.ASCENDING,
        "desc" to SorteerRichting.DESCENDING,
        "" to SorteerRichting.NONE
    ).forEach { (value, sorteerRichting) ->
        given("the sort direction value '$value'") {
            `when`("it is converted to a sort direction") {
                val convertedSorteerRichting = value.toSorteerRichting()

                then("it returns $sorteerRichting") {
                    convertedSorteerRichting shouldBe sorteerRichting
                }
            }
        }
    }

    given("an unknown sort direction value") {
        `when`("it is converted to a sort direction") {
            val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                "fakeSortDirection".toSorteerRichting()
            }

            then("it throws an exception that names the value") {
                illegalArgumentException.message shouldBe "Onbekende waarde 'fakeSortDirection'"
            }
        }
    }

    given("no sort direction value") {
        `when`("it is converted to a sort direction") {
            val illegalArgumentException = shouldThrow<IllegalArgumentException> {
                null.toSorteerRichting()
            }

            then("it throws an exception, because only an empty value means no sort direction") {
                illegalArgumentException.message shouldBe "Onbekende waarde 'null'"
            }
        }
    }
})
