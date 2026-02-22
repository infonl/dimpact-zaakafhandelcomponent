/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.PabcClientService
import nl.info.zac.util.AllOpen
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.LocalDateTime

@Readiness
@ApplicationScoped
@AllOpen
class PabcReadinessHealthCheck @Inject constructor(
    val pabcClientService: PabcClientService
) : HealthCheck {
    companion object {
        private const val FAKE_FUNCTIONAL_ROLE = "FAKE_FUNCTIONAL_ROLE"
    }

    /**
     * Attempt to call the PABC, using a (most likely non-existing) fake functional role name,
     * to ensure that the PABC is available and that the ZAC - PABC integration is properly configured.
     * If the call succeeds, return a HealthCheckResponse with status `up`.
     * If the call throws an exception, return a HealthCheckResponse with status `down`,
     * and include the error message from the exception.
     */
    @WithSpan(value = "GET PabcReadinessHealthCheck")
    override fun call(): HealthCheckResponse =
        try {
            pabcClientService.getApplicationRoles(listOf(FAKE_FUNCTIONAL_ROLE))
            HealthCheckResponse.up(PabcReadinessHealthCheck::class.java.name)
        } catch (@Suppress("TooGenericExceptionCaught") exception: Throwable) {
            HealthCheckResponse
                .named(PabcReadinessHealthCheck::class.java.name)
                .withData("time", LocalDateTime.now().toString())
                .withData("error", exception.message)
                .down()
                .build()
        }
}
