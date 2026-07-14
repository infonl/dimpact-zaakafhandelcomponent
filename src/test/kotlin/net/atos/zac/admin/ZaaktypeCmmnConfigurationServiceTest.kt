/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.shared.cache.Caching
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.util.UUID

class ZaaktypeCmmnConfigurationServiceTest : BehaviorSpec({
    val beheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()

    afterEach {
        checkUnnecessaryStub()
    }

    given("a zaaktype UUID") {
        val uuid = UUID.randomUUID()
        val config = createZaaktypeCmmnConfiguration(zaaktypeUUID = uuid)
        val service = ZaaktypeCmmnConfigurationService(beheerService)
        every { beheerService.fetchZaaktypeCmmnConfiguration(uuid) } returns config

        `when`("readZaaktypeCmmnConfiguration is called twice") {
            val first = service.readZaaktypeCmmnConfiguration(uuid)
            val second = service.readZaaktypeCmmnConfiguration(uuid)

            then("the config is returned and beheer service is called only once (cache hit)") {
                first shouldBe config
                second shouldBe config
                verify(exactly = 1) { beheerService.fetchZaaktypeCmmnConfiguration(uuid) }
            }
        }
    }

    given("a list of zaaktype CMMN configurations") {
        val configs = listOf(createZaaktypeCmmnConfiguration(), createZaaktypeCmmnConfiguration())
        val service = ZaaktypeCmmnConfigurationService(beheerService)
        every { beheerService.listZaaktypeCmmnConfiguration() } returns configs

        `when`("listZaaktypeCmmnConfiguration is called twice") {
            val first = service.listZaaktypeCmmnConfiguration()
            val second = service.listZaaktypeCmmnConfiguration()

            then("the list is returned and beheer service is called only once (cache hit)") {
                first shouldBe configs
                second shouldBe configs
                verify(exactly = 1) { beheerService.listZaaktypeCmmnConfiguration() }
            }
        }
    }

    given("a cached zaaktype configuration") {
        val uuid = UUID.randomUUID()
        val config = createZaaktypeCmmnConfiguration(zaaktypeUUID = uuid)
        val service = ZaaktypeCmmnConfigurationService(beheerService)
        every { beheerService.fetchZaaktypeCmmnConfiguration(uuid) } returns config

        `when`("cacheRemoveZaaktypeCmmnConfiguration is called after first read") {
            service.readZaaktypeCmmnConfiguration(uuid)
            service.cacheRemoveZaaktypeCmmnConfiguration(uuid)
            service.readZaaktypeCmmnConfiguration(uuid)

            then("beheer service is called twice because cache was invalidated") {
                verify(exactly = 2) { beheerService.fetchZaaktypeCmmnConfiguration(uuid) }
            }
        }
    }

    given("a populated managed cache") {
        val uuid = UUID.randomUUID()
        val config = createZaaktypeCmmnConfiguration(zaaktypeUUID = uuid)
        val service = ZaaktypeCmmnConfigurationService(beheerService)
        every { beheerService.fetchZaaktypeCmmnConfiguration(uuid) } returns config

        service.readZaaktypeCmmnConfiguration(uuid)

        `when`("clearManagedCache is called") {
            val result = service.clearManagedCache()

            then("return value contains cache name and beheer service is called again on next read") {
                result shouldContain Caching.ZAC_ZAAKTYPECMMNCONFIGURATION_MANAGED
                service.readZaaktypeCmmnConfiguration(uuid)
                verify(exactly = 2) { beheerService.fetchZaaktypeCmmnConfiguration(uuid) }
            }
        }
    }

    given("a populated list cache") {
        val configs = listOf(createZaaktypeCmmnConfiguration())
        val service = ZaaktypeCmmnConfigurationService(beheerService)
        every { beheerService.listZaaktypeCmmnConfiguration() } returns configs

        service.listZaaktypeCmmnConfiguration()

        `when`("clearListCache is called") {
            val result = service.clearListCache()

            then("return value contains cache name and beheer service is called again on next list") {
                result shouldContain Caching.ZAC_ZAAKTYPECMMNCONFIGURATION
                service.listZaaktypeCmmnConfiguration()
                verify(exactly = 2) { beheerService.listZaaktypeCmmnConfiguration() }
            }
        }
    }
})
