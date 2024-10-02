package net.atos.zac.app.util
import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.ZaakafhandelParameterService

class UtilRestServiceTest : BehaviorSpec({

    val ztcClientService = mockk<ZtcClientService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()

    Given("caches are empty") {
        val utilRESTService = UtilRestService(ztcClientService, zaakafhandelParameterService)

        every { ztcClientService.cacheStatistics() } returns mapOf(
            "ztc-cache1" to CacheStats.empty()
        )
        every { ztcClientService.cacheSizes() } returns mapOf(
            "ztc-cache1" to 0
        )
        every { zaakafhandelParameterService.cacheStatistics() } returns mapOf(
            "zafhPS-cache1" to CacheStats.empty()
        )
        every { zaakafhandelParameterService.cacheSizes() } returns mapOf(
            "zafhPS-cache1" to 0
        )

        When("cache statistics are requested") {
            val response = utilRESTService.caches()

            Then("stats are returned correctly") {
                response shouldContain "ztc-cache1"
                response shouldContain "zafhPS-cache1"
                response shouldContain "hitCount=0"
                response shouldContain "0 objects"
            }
        }
    }

    Given("util endpoint") {
        val utilRESTService = UtilRestService(ztcClientService, zaakafhandelParameterService)

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
                memoryResponse shouldContain "free"
                memoryResponse shouldContain "used"
                memoryResponse shouldContain "total"
                memoryResponse shouldContain "max"
            }
        }
    }
})
