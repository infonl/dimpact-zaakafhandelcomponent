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
import nl.info.zac.time.ZacTime
import org.eclipse.microprofile.health.HealthCheckResponse
import java.time.OffsetDateTime

class PabcReadinessHealthCheckTest : BehaviorSpec({
    val pabcClientService = mockk<PabcClientService>()
    val pabcReadinessHealthCheck = PabcReadinessHealthCheck(pabcClientService)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Calling the PABC readiness health check") {
        given(
            """
            The PABC client service returns a 'get application roles' response successfully
            """
        ) {
            every {
                pabcClientService.getApplicationRoles(listOf("FAKE_FUNCTIONAL_ROLE"))
            } returns createApplicationRolesResponse()

            `when`("the health check is called") {
                val healthCheckResponse = pabcReadinessHealthCheck.call()

                then("the health check should return UP status") {
                    healthCheckResponse.status shouldBe HealthCheckResponse.Status.UP
                    healthCheckResponse.name shouldBe "nl.info.zac.health.PabcReadinessHealthCheck"
                }
            }
        }

        given("The PABC client service throws an exception") {
            val runtimeException = RuntimeException("fakeError")
            every { pabcClientService.getApplicationRoles(listOf("FAKE_FUNCTIONAL_ROLE")) } throws runtimeException

            `when`("the health check is called") {
                val healthCheckResponse = pabcReadinessHealthCheck.call()

                then("the health check returns DOWN with the error and the time with the offset of Europe/Amsterdam") {
                    healthCheckResponse.status shouldBe HealthCheckResponse.Status.DOWN
                    healthCheckResponse.name shouldBe "nl.info.zac.health.PabcReadinessHealthCheck"

                    with(healthCheckResponse.data.get()) {
                        get("error") shouldBe runtimeException.message
                        val time = OffsetDateTime.parse(get("time").toString())
                        time.offset shouldBe ZacTime.ZONE.rules.getOffset(time.toInstant())
                    }
                }
            }
        }
    }
})
