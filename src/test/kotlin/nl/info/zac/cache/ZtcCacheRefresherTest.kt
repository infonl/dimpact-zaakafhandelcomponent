/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.cache

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.authentication.LoggedInUserProvider
import nl.info.zac.configuration.ConfigurationService
import java.net.URI

class ZtcCacheRefresherTest : BehaviorSpec({
    val catalogusUri = URI("https://example.com/catalogussen/fakeCatalogusUuid")

    afterEach {
        checkUnnecessaryStub()
    }

    given("a default catalogus") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(ztcClientService, configurationService, testDispatcher)
        var isRefreshedAsSystemUser = false
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } answers {
            isRefreshedAsSystemUser = LoggedInUserProvider.systemUser.get()
        }

        `when`("ZAC has started") {
            ztcCacheRefresher.onStartup(Any())

            then(
                """the caches of the default catalogus are refreshed as the system user in the background,
                so that startup does not wait for the refresh"""
            ) {
                verify(exactly = 0) { ztcClientService.refreshCaches(any()) }

                testDispatcher.scheduler.advanceUntilIdle()

                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }
                isRefreshedAsSystemUser shouldBe true
            }
        }
    }

    given("ZAC has started and the refresh after startup has finished") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(ztcClientService, configurationService, testDispatcher)
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } returns Unit
        ztcCacheRefresher.onStartup(Any())
        testDispatcher.scheduler.advanceUntilIdle()

        `when`("a refresh in the background is requested") {
            ztcCacheRefresher.refreshInBackground()

            then("the caches are refreshed again in the background") {
                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }

                testDispatcher.scheduler.advanceUntilIdle()

                verify(exactly = 2) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }

    given("a refresh that is running") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(ztcClientService, configurationService, testDispatcher)
        var refreshCount = 0
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } answers {
            if (++refreshCount == 1) {
                ztcCacheRefresher.refreshInBackground()
                ztcCacheRefresher.refreshInBackground()
                ztcCacheRefresher.refreshInBackground()
            }
        }

        `when`("several refreshes are requested while it runs") {
            ztcCacheRefresher.onStartup(Any())
            testDispatcher.scheduler.advanceUntilIdle()

            then(
                """the requests are combined into one more refresh after the running one, so that changes the
                running refresh may have missed are cached, without a refresh per request"""
            ) {
                refreshCount shouldBe 2
            }
        }
    }

    given("a refresh that fails") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(ztcClientService, configurationService, testDispatcher)
        every { configurationService.readDefaultCatalogusURI() } throws RuntimeException("fakeFailure") andThen
            catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } returns Unit
        ztcCacheRefresher.onStartup(Any())
        testDispatcher.scheduler.advanceUntilIdle()

        `when`("a refresh in the background is requested afterwards") {
            ztcCacheRefresher.refreshInBackground()
            testDispatcher.scheduler.advanceUntilIdle()

            then("the caches are still refreshed") {
                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }
})
