/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.net.URI

class ResultsTest : BehaviorSpec({
    given("the two-argument Results constructor") {
        val results = Results(listOf("fakeItem"), 1)

        `when`("count, results, next and previous are read") {
            val count = results.count()
            val resultsList = results.results()
            val next = results.next()
            val previous = results.previous()

            then("count and results reflect the arguments, next and previous default to null") {
                count shouldBe 1
                resultsList shouldBe listOf("fakeItem")
                next.shouldBeNull()
                previous.shouldBeNull()
            }
        }
    }

    given("a Results with a next page URI") {
        val results = Results(countValue = 2, resultsValue = listOf("fakeItem"), nextValue = URI("https://example.com/next"))

        `when`("next is read") {
            val next = results.next()

            then("it returns the configured URI") {
                next shouldBe URI("https://example.com/next")
            }
        }

        `when`("singlePageResults is read") {
            val exception = shouldThrow<IllegalStateException> { results.singlePageResults }

            then("it throws because there is more than one page") {
                exception.message shouldBe "More than one page found (count: 2, results: 1)"
            }
        }
    }

    given("a Results without a next page") {
        val results = Results(listOf("fakeItem"), 1)

        `when`("singlePageResults is read") {
            val singlePageResults = results.singlePageResults

            then("it returns the results") {
                singlePageResults shouldBe listOf("fakeItem")
            }
        }
    }

    given("a Results with no items") {
        val results = Results(emptyList<String>(), 0)

        `when`("singleResult is read") {
            val singleResult = results.singleResult

            then("it returns null") {
                singleResult.shouldBeNull()
            }
        }
    }

    given("a Results with exactly one item") {
        val results = Results(listOf("fakeItem"), 1)

        `when`("singleResult is read") {
            val singleResult = results.singleResult

            then("it returns that item") {
                singleResult shouldBe "fakeItem"
            }
        }
    }

    given("a Results with more than one item") {
        val results = Results(listOf("fakeItem1", "fakeItem2"), 2)

        `when`("singleResult is read") {
            val exception = shouldThrow<IllegalStateException> { results.singleResult }

            then("it throws because there is more than one result") {
                exception.message shouldBe "More than one result found (count: 2)"
            }
        }
    }
})
