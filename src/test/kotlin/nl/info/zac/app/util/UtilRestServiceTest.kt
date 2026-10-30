/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldInclude
import io.kotest.matchers.string.shouldMatch
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.sensitive.SensitiveDataService

class UtilRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val policyService = mockk<PolicyService>()
    val sensitiveDataService = mockk<SensitiveDataService>()
    val utilRESTService = UtilRestService(
        ztcClientService = ztcClientService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        sensitiveDataService = sensitiveDataService,
        policyService = policyService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("caches are empty") {
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { ztcClientService.cacheStatistics() } returns mapOf(
            "ztc-cache1" to CacheStats.empty()
        )
        every { ztcClientService.estimatedCacheSizes() } returns mapOf(
            "ztc-cache1" to 0
        )
        every { zaaktypeConfigurationService.cacheStatistics() } returns mapOf(
            "zafhPS-cache1" to CacheStats.empty()
        )
        every { zaaktypeConfigurationService.estimatedCacheSizes() } returns mapOf(
            "zafhPS-cache1" to 0
        )

        `when`("cache statistics are requested") {
            val response = utilRESTService.caches()

            then("stats are returned correctly, without measuring the memory usage") {
                verify(exactly = 0) { ztcClientService.estimatedMemoryUsage() }
                response shouldContain "ztc-cache1"
                response shouldContain "zafhPS-cache1"
                response shouldContain "hitCount=0"
                response shouldContain "Estimated cache size: 0 item(s)"
            }
        }
    }

    given("A user with 'beheren' permissions") {
        every { policyService.readOverigeRechten().canBeheren } returns true

        `when`("index is requested") {
            val indexResponse = utilRESTService.index()

            then("it returns caches and memory links") {
                indexResponse shouldContain "cache"
                indexResponse shouldContain "clear"
                indexResponse shouldContain "memory"
            }
        }

        `when`("all caches are cleared") {
            every { ztcClientService.clearZaaktypeCache() } returns "zaaktype-cache cleared"
            every { ztcClientService.clearResultaattypeCache() } returns "resultaattype-cache cleared"
            every { ztcClientService.clearStatustypeCache() } returns "statustype-cache cleared"
            every { ztcClientService.clearInformatieobjecttypeCache() } returns "informatieobjecttype-cache cleared"
            every {
                ztcClientService.clearZaaktypeInformatieobjecttypeCache()
            } returns "zaaktype-informatieobjecttype-cache cleared"
            every { ztcClientService.clearBesluittypeCache() } returns "besluittype-cache cleared"
            every { ztcClientService.clearRoltypeCache() } returns "roltype-cache cleared"
            every { ztcClientService.clearEigenschapCache() } returns "eigenschap-cache cleared"
            every { ztcClientService.clearCacheTime() } returns "cachetime cleared"
            every { zaaktypeConfigurationService.clearListCache() } returns "zaaktype-cmmn-cache cleared"
            every { zaaktypeConfigurationService.clearManagedCache() } returns "zaaktype-cmmn-managed-cache cleared"

            val clearResponse = utilRESTService.clearCaches()

            then("it should clear all caches") {
                verify(exactly = 1) {
                    ztcClientService.clearZaaktypeCache()
                    ztcClientService.clearResultaattypeCache()
                    ztcClientService.clearStatustypeCache()
                    ztcClientService.clearInformatieobjecttypeCache()
                    ztcClientService.clearZaaktypeInformatieobjecttypeCache()
                    ztcClientService.clearBesluittypeCache()
                    ztcClientService.clearRoltypeCache()
                    ztcClientService.clearEigenschapCache()
                    ztcClientService.clearCacheTime()
                    zaaktypeConfigurationService.clearListCache()
                    zaaktypeConfigurationService.clearManagedCache()
                }
            }
            and("sensitive data should not be cleared") {
                verify(exactly = 0) { sensitiveDataService.clearStorage() }
            }
            and("response should contain all results") {
                clearResponse.windowed("cleared".length) { it == "cleared" }.count { it } shouldBe 11
            }
        }

        `when`("sensitive data clear is requested") {
            every { sensitiveDataService.clearStorage() } returns "cache cleared"
            val clearResponse = utilRESTService.clearAllSensitiveDataCaches()

            then("it should clear all sensitive data caches") {
                clearResponse shouldInclude "cache cleared"
            }
        }

        `when`("memory info is requested") {
            val memoryResponse = utilRESTService.memory()

            then("free, used, total and max memory are shown") {
                memoryResponse shouldMatch """
                    <html></head><body><h1>Memory</h1><ul><li>free: \d+.\d+ .* \(.*\)</li><li>used : \d+.\d+ .* \(.*\)</li><li>total: \d+.\d+ .* \(.*\)</li><li>max  : \d+.\d+ .* \(.*\)</li></ul></body></html>
                """.trimIndent()
            }
        }

        `when`("the ZTC caches are requested") {
            every { ztcClientService.cacheStatistics() } returns mapOf(
                "fakeCacheName1" to CacheStats.empty(),
                "fakeCacheName2" to CacheStats.empty()
            )
            every { ztcClientService.estimatedCacheSizes() } returns mapOf(
                "fakeCacheName1" to 1L,
                "fakeCacheName2" to 2L
            )
            every { ztcClientService.estimatedMemoryUsage() } returns mapOf(
                "fakeCacheName1" to 1_000L,
                "fakeCacheName2" to 2_000L
            )
            val ztcCachesResponse = utilRESTService.ztcCaches()

            then("the total memory usage and the size and memory usage per cache are shown") {
                ztcCachesResponse shouldContain "Estimated memory usage of all caches: 2.93 kB (3000 bytes)"
                ztcCachesResponse shouldContain "Estimated cache size: 1 item(s)</li><li>Estimated memory usage: 1000 B (1000 bytes)"
                ztcCachesResponse shouldContain "Estimated cache size: 2 item(s)</li><li>Estimated memory usage: 1.95 kB (2000 bytes)"
            }
        }
    }

    given("A user without 'beheren' permissions") {
        every { policyService.readOverigeRechten().canBeheren } returns false

        `when`("index is requested") {
            val exception = shouldThrow<PolicyException> {
                utilRESTService.index()
            }

            then("it should throw an exception") {
                exception shouldNotBe null
            }
        }

        `when`("memory info is requested") {
            val exception = shouldThrow<PolicyException> {
                utilRESTService.memory()
            }

            then("it should throw an exception") {
                exception shouldNotBe null
            }
        }

        `when`("the ZTC caches are requested") {
            val exception = shouldThrow<PolicyException> {
                utilRESTService.ztcCaches()
            }

            then("it should throw an exception without measuring the caches") {
                exception shouldNotBe null
                verify(exactly = 0) { ztcClientService.estimatedMemoryUsage() }
            }
        }
    }
})
