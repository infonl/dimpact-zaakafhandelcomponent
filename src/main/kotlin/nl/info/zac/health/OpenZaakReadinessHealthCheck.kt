/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.CatalogusListParameters
import nl.info.zac.configuratie.ConfigurationService
import nl.info.zac.util.AllOpen
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.LocalDateTime

@Readiness
@ApplicationScoped
@AllOpen
class OpenZaakReadinessHealthCheck @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configurationService: ConfigurationService
) : HealthCheck {

    @WithSpan(value = "GET OpenZaakReadinessHealthCheck")
    override fun call(): HealthCheckResponse =
        try {
            configurationService.readCatalogusDomein()
                .let { domein -> CatalogusListParameters().also { it.domein = domein } }
                .let(ztcClientService::listCatalogus)
            HealthCheckResponse.up(OpenZaakReadinessHealthCheck::class.java.name)
        } catch (@Suppress("TooGenericExceptionCaught") exception: RuntimeException) {
            HealthCheckResponse.named(OpenZaakReadinessHealthCheck::class.java.name)
                .withData("time", LocalDateTime.now().toString())
                .withData("error", exception.message)
                .down()
                .build()
        }
}
