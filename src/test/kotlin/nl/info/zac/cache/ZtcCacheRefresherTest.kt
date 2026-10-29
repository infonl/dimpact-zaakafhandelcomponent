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
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val testDispatcher = StandardTestDispatcher()
    val ztcCacheRefresher = ZtcCacheRefresher(
        ztcClientService = ztcClientService,
        configurationService = configurationService,
        dispatcher = testDispatcher
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("a default catalogus") {
        val catalogusUri = URI("https://example.com/catalogussen/fakeCatalogusUuid")
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
})
