/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.cache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private class FakeCaching : Caching {
    override fun cacheStatistics(): Map<String, CacheStats> = emptyMap()

    override fun estimatedCacheSizes(): Map<String, Long> = emptyMap()
}

class CachingTest : BehaviorSpec({
    val caching = FakeCaching()

    given("a cache name") {
        `when`("cleared is called") {
            val message = caching.cleared("fake-cache")

            then("it returns a message confirming the cache was cleared") {
                message shouldBe "fake-cache cache cleared"
            }
        }

        `when`("removed is called with a cache key") {
            then("it does not throw") {
                shouldNotThrowAny { caching.removed("fake-cache", "fake-key") }
            }
        }
    }
})
