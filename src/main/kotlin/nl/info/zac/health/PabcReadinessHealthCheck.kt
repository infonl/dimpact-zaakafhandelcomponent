/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.pabc.PabcClientService
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.util.AllOpen
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.LocalDateTime

@Readiness
@ApplicationScoped
@AllOpen
class PabcReadinessHealthCheck @Inject constructor(
    private val pabcClientService: PabcClientService,
    private val configurationService: ConfigurationService
) : HealthCheck {
    companion object {
        private const val FAKE_FUNCTIONAL_ROLE = "FAKE_FUNCTIONAL_ROLE"
    }

    /**
     * If the PABC feature flag is enabled, attempt to call the PABC get application roles endpoint,
     * with a (most likely non-existing) fake functional role name,
     * to ensure that the PABC is available and that the ZAC - PABC integration is properly configured.
     * If the call succeeds or if the PABC feature flag is disabled, return a HealthCheckResponse with status `up`.
     * If the call throws an exception, return a HealthCheckResponse with status `down`,
     * and include the error message from the exception.
     */
    @WithSpan(value = "GET PabcReadinessHealthCheck")
    override fun call(): HealthCheckResponse =
        if (configurationService.featureFlagPabcIntegration()) {
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
        } else {
            HealthCheckResponse.up(PabcReadinessHealthCheck::class.java.name)
        }
}
