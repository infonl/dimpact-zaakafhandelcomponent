/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.klant.model.Klant;
import net.atos.client.klant.model.KlantList200Response;
import net.atos.client.klant.model.KlantListParameters;
import net.atos.zac.configuratie.ConfiguratieService;

@Singleton
public class KlantClientService {

    @Inject
    @RestClient
    private KlantClient klantClient;

    public Optional<Klant> findPersoon(final String bsn) {
        return convertToSingleItem(klantClient.klantList(createFindPersoonListParameters(bsn)));
    }

    public CompletionStage<Optional<Klant>> findPersoonAsync(final String bsn) {
        return klantClient.klantListAsync(createFindPersoonListParameters(bsn))
                .thenApply(this::convertToSingleItem);
    }

    public Optional<Klant> findVestiging(final String vestigingsnummer) {
        return convertToSingleItem(klantClient.klantList(createFindVestigingListParameters(vestigingsnummer)));
    }

    public CompletionStage<Optional<Klant>> findVestigingAsync(final String vestigingsnummer) {
        return klantClient.klantListAsync(createFindVestigingListParameters(vestigingsnummer))
                .thenApply(this::convertToSingleItem);
    }

    private KlantListParameters createFindPersoonListParameters(final String bsn) {
        final KlantListParameters klantListParameters = createKlantListParameters();
        klantListParameters.setSubjectNatuurlijkPersoonInpBsn(bsn);
        return klantListParameters;
    }

    private KlantListParameters createFindVestigingListParameters(final String vestigingsnummer) {
        final KlantListParameters klantListParameters = createKlantListParameters();
        klantListParameters.setSubjectVestigingVestigingsNummer(vestigingsnummer);
        return klantListParameters;
    }

    private KlantListParameters createKlantListParameters() {
        final KlantListParameters klantListParameters = new KlantListParameters();
        klantListParameters.setBronorganisatie(ConfiguratieService.BRON_ORGANISATIE);
        return klantListParameters;
    }

    private Optional<Klant> convertToSingleItem(final KlantList200Response response) {
        return switch (response.getResults().size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(response.getResults().getFirst());
            default -> throw new IllegalStateException("Too many results: %d".formatted(response.getResults().size()));
        };
    }
}
