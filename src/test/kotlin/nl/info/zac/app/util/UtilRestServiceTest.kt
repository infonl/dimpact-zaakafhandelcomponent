/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.ztc.ZtcClientService

class UtilRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val policyService = mockk<PolicyService>()
    val utilRESTService = UtilRestService(
        ztcClientService = ztcClientService,
        zaakafhandelParameterService = zaakafhandelParameterService,
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
        every { zaakafhandelParameterService.cacheStatistics() } returns mapOf(
            "zafhPS-cache1" to CacheStats.empty()
        )
        every { zaakafhandelParameterService.estimatedCacheSizes() } returns mapOf(
            "zafhPS-cache1" to 0
        )

        When("cache statistics are requested") {
            val response = utilRESTService.caches()

            Then("stats are returned correctly") {
                response shouldContain "ztc-cache1"
                response shouldContain "zafhPS-cache1"
                response shouldContain "hitCount=0"
                response shouldContain "Estimated cache size: 0"
            }
        }
    }

    Given("util endpoint") {
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
})
