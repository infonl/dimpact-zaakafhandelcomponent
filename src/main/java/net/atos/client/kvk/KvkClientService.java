/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk;

import java.util.Collections;
import java.util.Optional;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.kvk.exception.KvkClientNoResultException;
import net.atos.client.kvk.model.KvkZoekenParameters;
import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging;
import net.atos.client.kvk.zoeken.model.generated.Resultaat;
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem;

@ApplicationScoped
public class KvkClientService {
    private static final Logger LOG = Logger.getLogger(KvkClientService.class.getName());

    private ZoekenClient zoekenClient;
    private VestigingsprofielClient vestigingsprofielClient;

    @Inject
    public KvkClientService(
            @RestClient ZoekenClient zoekenClient,
            @RestClient VestigingsprofielClient vestigingsprofielClient
    ) {
        this.zoekenClient = zoekenClient;
        this.vestigingsprofielClient = vestigingsprofielClient;
    }

    /**
     * Default no-arg constructor, required by Weld.
     */
    public KvkClientService() {
    }

    public Resultaat list(final KvkZoekenParameters parameters) {
        try {
            return zoekenClient.getResults(parameters);
        } catch (final KvkClientNoResultException exception) {
            // Nothing to report
        } catch (final RuntimeException exception) {
            LOG.warning(() -> ("Failed to search for company information using the KVK API: %s").formatted(exception));
        }
        return createEmptyResultaat();
    }

    public Optional<Vestiging> findVestigingsprofiel(final String vestigingsnummer) {
        return Optional.of(vestigingsprofielClient.getVestigingByVestigingsnummer(vestigingsnummer, false));
    }

    public Optional<ResultaatItem> findHoofdvestiging(final String kvkNummer) {
        final KvkZoekenParameters zoekParameters = new KvkZoekenParameters();
        zoekParameters.setType("hoofdvestiging");
        zoekParameters.setKvkNummer(kvkNummer);
        return convertToSingleItem(list(zoekParameters));
    }

    public Optional<ResultaatItem> findVestiging(final String vestigingsnummer) {
        final KvkZoekenParameters zoekParameters = new KvkZoekenParameters();
        zoekParameters.setVestigingsnummer(vestigingsnummer);
        return convertToSingleItem(list(zoekParameters));
    }

    public Optional<ResultaatItem> findRechtspersoon(final String rsin) {
        final KvkZoekenParameters zoekParameters = new KvkZoekenParameters();
        zoekParameters.setType("rechtspersoon");
        zoekParameters.setRsin(rsin);
        return convertToSingleItem(list(zoekParameters));
    }

    private Optional<ResultaatItem> convertToSingleItem(final Resultaat resultaat) {
        return switch (resultaat.getTotaal()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(resultaat.getResultaten().getFirst());
            default -> throw new IllegalStateException("Too many results: %d".formatted(resultaat.getTotaal()));
        };
    }

    private Resultaat createEmptyResultaat() {
        final Resultaat resultaat = new Resultaat();
        resultaat.setTotaal(0);
        resultaat.setResultaten(Collections.emptyList());
        return resultaat;
    }

    private Resultaat handleListAsync(final Resultaat resultaat, final Throwable exception) {
        if (resultaat != null) {
            return resultaat;
        } else {
            if (!(exception instanceof KvkClientNoResultException)) {
                LOG.warning(() -> "Error while calling listAsync: %s".formatted(exception.getMessage()));
            }
            return createEmptyResultaat();
        }
    }
}
