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
            then("count and results reflect the arguments, next and previous default to null") {
                results.count() shouldBe 1
                results.results() shouldBe listOf("fakeItem")
                results.next().shouldBeNull()
                results.previous().shouldBeNull()
            }
        }
    }

    given("a Results with a next page URI") {
        val results = Results(countValue = 2, resultsValue = listOf("fakeItem"), nextValue = URI("https://example.com/next"))

        `when`("next is read") {
            then("it returns the configured URI") {
                results.next() shouldBe URI("https://example.com/next")
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
            then("it returns the results") {
                results.singlePageResults shouldBe listOf("fakeItem")
            }
        }
    }

    given("a Results with no items") {
        val results = Results(emptyList<String>(), 0)

        `when`("singleResult is read") {
            then("it returns null") {
                results.singleResult.shouldBeNull()
            }
        }
    }

    given("a Results with exactly one item") {
        val results = Results(listOf("fakeItem"), 1)

        `when`("singleResult is read") {
            then("it returns that item") {
                results.singleResult shouldBe "fakeItem"
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
