/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import jakarta.enterprise.context.ApplicationScoped
import nl.info.zac.util.AllOpen
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness

@Liveness
@ApplicationScoped
@AllOpen
class LivenessHealthCheck : HealthCheck {

    override fun call(): HealthCheckResponse =
        LivenessHealthCheck::class.java.name.let(HealthCheckResponse::up)
}
