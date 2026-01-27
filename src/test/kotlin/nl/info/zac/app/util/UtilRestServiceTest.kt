/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.sensitive.SensitiveDataService

class UtilRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val policyService = mockk<PolicyService>()
    val sensitiveDataService = mockk<SensitiveDataService>()
    val utilRESTService = UtilRestService(
        ztcClientService = ztcClientService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        sensitiveDataService = sensitiveDataService,
        policyService = policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("caches are empty") {
        every { policyService.readOverigeRechten().beheren } returns true
        every { ztcClientService.cacheStatistics() } returns mapOf(
            "ztc-cache1" to CacheStats.empty()
        )
        every { ztcClientService.estimatedCacheSizes() } returns mapOf(
            "ztc-cache1" to 0
        )
        every { zaaktypeCmmnConfigurationService.cacheStatistics() } returns mapOf(
            "zafhPS-cache1" to CacheStats.empty()
        )
        every { zaaktypeCmmnConfigurationService.estimatedCacheSizes() } returns mapOf(
            "zafhPS-cache1" to 0
        )

        When("cache statistics are requested") {
            val response = utilRESTService.caches()

            Then("stats are returned correctly") {
                response shouldContain "ztc-cache1"
                response shouldContain "zafhPS-cache1"
                response shouldContain "hitCount=0"
                response shouldContain "sensitive"
                response shouldContain "Estimated cache size: 0"
            }
        }
    }

    Given("A user with 'beheren' permissions") {
        every { policyService.readOverigeRechten().beheren } returns true

        When("index is requested") {
            val indexResponse = utilRESTService.index()

            Then("it returns caches and memory links") {
                indexResponse shouldContain "cache"
                indexResponse shouldContain "clear"
                indexResponse shouldContain "memory"
            }
        }

        When("memory info is requested") {
            val memoryResponse = utilRESTService.memory()

            Then("free, used, total and max memory are shown") {
                memoryResponse shouldMatch """ 
                    <html></head><body><h1>Memory</h1><ul><li>free: \d+.\d+ .* \(.*\)</li><li>used : \d+.\d+ .* \(.*\)</li><li>total: \d+.\d+ .* \(.*\)</li><li>max  : \d+.\d+ .* \(.*\)</li></ul></body></html>
                """.trimIndent()
            }
        }
    }

    Given("A user without 'beheren' permissions") {
        every { policyService.readOverigeRechten().beheren } returns false

        When("index is requested") {
            val exception = shouldThrow<PolicyException> {
                utilRESTService.index()
            }

            Then("it should throw an exception") {
                exception shouldNotBe null
            }
        }

        When("memory info is requested") {
            val exception = shouldThrow<PolicyException> {
                utilRESTService.memory()
            }

            Then("it should throw an exception") {
                exception shouldNotBe null
            }
        }
    }
})
