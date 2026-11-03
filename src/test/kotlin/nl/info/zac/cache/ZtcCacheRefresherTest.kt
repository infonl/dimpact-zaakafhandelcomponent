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
import kotlin.time.Duration.Companion.seconds

class ZtcCacheRefresherTest : BehaviorSpec({
    val catalogusUri = URI("https://example.com/catalogussen/fakeCatalogusUuid")
    val refreshIntervalSeconds = 3600L

    afterEach {
        checkUnnecessaryStub()
    }

    given("a default catalogus") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = refreshIntervalSeconds,
            dispatcher = testDispatcher
        )
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

                testDispatcher.scheduler.runCurrent()

                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }
                isRefreshedAsSystemUser shouldBe true
            }
        }
    }

    given("ZAC has started and the refresh after startup has finished") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = refreshIntervalSeconds,
            dispatcher = testDispatcher
        )
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } returns Unit
        ztcCacheRefresher.onStartup(Any())
        testDispatcher.scheduler.runCurrent()

        `when`("a refresh in the background is requested") {
            ztcCacheRefresher.refreshInBackground()

            then("the caches are refreshed again in the background") {
                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }

                testDispatcher.scheduler.runCurrent()

                verify(exactly = 2) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }

    given("ZAC has started with a refresh interval") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = refreshIntervalSeconds,
            dispatcher = testDispatcher
        )
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } returns Unit
        ztcCacheRefresher.onStartup(Any())
        testDispatcher.scheduler.runCurrent()

        `when`("the refresh interval has passed twice") {
            then("the caches are refreshed in the background after each interval, and not before") {
                testDispatcher.scheduler.advanceTimeBy((refreshIntervalSeconds - 1).seconds)
                testDispatcher.scheduler.runCurrent()
                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }

                testDispatcher.scheduler.advanceTimeBy(1.seconds)
                testDispatcher.scheduler.runCurrent()
                verify(exactly = 2) { ztcClientService.refreshCaches(catalogusUri) }

                testDispatcher.scheduler.advanceTimeBy(refreshIntervalSeconds.seconds)
                testDispatcher.scheduler.runCurrent()
                verify(exactly = 3) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }

    given("ZAC has started with a refresh interval of 0") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = 0,
            dispatcher = testDispatcher
        )
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } returns Unit
        ztcCacheRefresher.onStartup(Any())

        `when`("a long time has passed") {
            testDispatcher.scheduler.advanceUntilIdle()

            then("the caches are only refreshed after startup, because the periodic refresh is turned off") {
                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }

    given("a refresh that is running") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = refreshIntervalSeconds,
            dispatcher = testDispatcher
        )
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
            testDispatcher.scheduler.runCurrent()

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
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = refreshIntervalSeconds,
            dispatcher = testDispatcher
        )
        every { configurationService.readDefaultCatalogusURI() } throws RuntimeException("fakeFailure") andThen
            catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } returns Unit
        ztcCacheRefresher.onStartup(Any())
        testDispatcher.scheduler.runCurrent()

        `when`("a refresh in the background is requested afterwards") {
            ztcCacheRefresher.refreshInBackground()
            testDispatcher.scheduler.runCurrent()

            then("the caches are still refreshed") {
                verify(exactly = 1) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }

    given("a refresh that fails with an error instead of an exception") {
        val ztcClientService = mockk<ZtcClientService>()
        val configurationService = mockk<ConfigurationService>()
        val testDispatcher = StandardTestDispatcher()
        val ztcCacheRefresher = ZtcCacheRefresher(
            ztcClientService = ztcClientService,
            configurationService = configurationService,
            refreshIntervalSeconds = refreshIntervalSeconds,
            dispatcher = testDispatcher
        )
        every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
        every { ztcClientService.refreshCaches(catalogusUri) } throws StackOverflowError("fakeError") andThen Unit
        ztcCacheRefresher.onStartup(Any())
        testDispatcher.scheduler.runCurrent()

        `when`("a refresh in the background is requested afterwards") {
            ztcCacheRefresher.refreshInBackground()
            testDispatcher.scheduler.runCurrent()

            then("the caches are still refreshed, because the error does not stop the handling of refresh requests") {
                verify(exactly = 2) { ztcClientService.refreshCaches(catalogusUri) }
            }
        }
    }
})
