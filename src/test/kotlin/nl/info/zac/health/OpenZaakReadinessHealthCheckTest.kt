/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Results
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.client.zgw.ztc.model.generated.Catalogus
import nl.info.zac.configuration.ConfigurationService
import org.eclipse.microprofile.health.HealthCheckResponse

class OpenZaakReadinessHealthCheckTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val healthCheck = OpenZaakReadinessHealthCheck(ztcClientService, configurationService)

    afterEach { checkUnnecessaryStub() }

    context("OpenZaak is reachable") {
        given("ZtcClientService.listCatalogus returns successfully") {
            every { configurationService.readCatalogusDomein() } returns "fakeDomein"
            every { ztcClientService.listCatalogus(any<CatalogusListParameters>()) } returns mockk<Results<Catalogus>>()

            `when`("call() is invoked") {
                val result = healthCheck.call()

                then("a HealthCheckResponse with status UP is returned") {
                    result.status shouldBe HealthCheckResponse.Status.UP
                }
            }
        }
    }

    context("OpenZaak is unreachable") {
        given("ZtcClientService.listCatalogus throws a RuntimeException") {
            val fakeException = RuntimeException("fakeConnectionError")
            every { configurationService.readCatalogusDomein() } returns "fakeDomein"
            every { ztcClientService.listCatalogus(any<CatalogusListParameters>()) } throws fakeException

            `when`("call() is invoked") {
                val result = healthCheck.call()

                then("a HealthCheckResponse with status DOWN is returned") {
                    result.status shouldBe HealthCheckResponse.Status.DOWN
                }

                And("the error data contains the exception message") {
                    result.data.get()["error"] shouldBe "fakeConnectionError"
                }
            }
        }
    }
})
