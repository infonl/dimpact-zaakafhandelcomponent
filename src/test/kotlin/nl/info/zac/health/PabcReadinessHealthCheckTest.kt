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
import nl.info.client.pabc.PabcClientService
import nl.info.client.pabc.model.createApplicationRolesResponse
import org.eclipse.microprofile.health.HealthCheckResponse

class PabcReadinessHealthCheckTest : BehaviorSpec({
    val pabcClientService = mockk<PabcClientService>()
    val pabcReadinessHealthCheck = PabcReadinessHealthCheck(pabcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Calling the PABC readiness health check") {
        Given("PABC client service returns a 'get application roles' response successfully") {
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

        Given("PABC client service throws an exception") {
            val runtimeException = RuntimeException("fakeError")
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
    }
})
