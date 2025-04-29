/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import java.time.LocalDateTime;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import nl.info.client.zgw.ztc.ZtcClientService;
import nl.info.client.zgw.ztc.model.CatalogusListParameters;
import nl.info.zac.configuratie.ConfiguratieService;

@Readiness
@ApplicationScoped
public class OpenZaakReadinessHealthCheck implements HealthCheck {

    private static final CatalogusListParameters CATALOGUS_LIST_PARAMETERS = new CatalogusListParameters();

    private final ZtcClientService ztcClientService;

    @Inject
    OpenZaakReadinessHealthCheck(ZtcClientService ztcClientService, ConfiguratieService configuratieService) {
        this.ztcClientService = ztcClientService;

        CATALOGUS_LIST_PARAMETERS.setDomein(configuratieService.readCatalogusDomein());
    }

    @Override
    @WithSpan(value = "GET OpenZaakReadinessHealthCheck")
    public HealthCheckResponse call() {
        try {
            ztcClientService.listCatalogus(CATALOGUS_LIST_PARAMETERS);
            return HealthCheckResponse.up(OpenZaakReadinessHealthCheck.class.getName());
        } catch (final Exception exception) {
            return HealthCheckResponse.named(OpenZaakReadinessHealthCheck.class.getName())
                    .withData("time", LocalDateTime.now().toString())
                    .withData("error", exception.getMessage())
                    .down()
                    .build();
        }
    }
}
