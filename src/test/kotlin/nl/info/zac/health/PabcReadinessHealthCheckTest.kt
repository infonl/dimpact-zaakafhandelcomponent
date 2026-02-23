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
import io.mockk.verify
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.createApplicationRolesResponse
import nl.info.zac.configuration.ConfigurationService
import org.eclipse.microprofile.health.HealthCheckResponse

class PabcReadinessHealthCheckTest : BehaviorSpec({
    val pabcClientService = mockk<PabcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val pabcReadinessHealthCheck = PabcReadinessHealthCheck(pabcClientService, configurationService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Calling the PABC readiness health check") {
        Given(
            """
            The PABC feature flag is enabled and the PABC client service returns 
            a 'get application roles' response successfully
            """
        ) {
            every { configurationService.featureFlagPabcIntegration() } returns true
            every {
                pabcClientService.getApplicationRoles(listOf("FAKE_FUNCTIONAL_ROLE"))
            } returns createApplicationRolesResponse()

            When("the health check is called") {
                val healthCheckResponse = pabcReadinessHealthCheck.call()

                Then("the health check should return UP status") {
                    healthCheckResponse.status shouldBe HealthCheckResponse.Status.UP
                    healthCheckResponse.name shouldBe "nl.info.zac.health.PabcReadinessHealthCheck"
                }
            }
        }

        Given("The PABC feature flag is enabled and PABC client service throws an exception") {
            val runtimeException = RuntimeException("fakeError")
            every { configurationService.featureFlagPabcIntegration() } returns true
            every { pabcClientService.getApplicationRoles(listOf("FAKE_FUNCTIONAL_ROLE")) } throws runtimeException

            When("the health check is called") {
                val healthCheckResponse = pabcReadinessHealthCheck.call()

                Then("the health check should return DOWN status with error") {
                    healthCheckResponse.status shouldBe HealthCheckResponse.Status.DOWN
                    healthCheckResponse.name shouldBe "nl.info.zac.health.PabcReadinessHealthCheck"

                    with(healthCheckResponse.data.get()) {
                        get("error") shouldBe runtimeException.message
                        containsKey("time") shouldBe true
                    }
                }
            }
        }

        Given("The PABC feature flag is disabled") {
            every { configurationService.featureFlagPabcIntegration() } returns false

            When("the health check is called") {
                val healthCheckResponse = pabcReadinessHealthCheck.call()

                Then("the health check should not call the PABC") {
                    verify(exactly = 0) { pabcClientService.getApplicationRoles(any()) }
                }

                And("it should return UP status") {
                    healthCheckResponse.status shouldBe HealthCheckResponse.Status.UP
                    healthCheckResponse.name shouldBe "nl.info.zac.health.PabcReadinessHealthCheck"
                }
            }
        }
    }
})
