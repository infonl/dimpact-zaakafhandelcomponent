/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import nl.info.client.klanten.model.generated.Betrokkene;
import nl.info.client.klanten.model.generated.DigitaalAdres;
import nl.info.client.klanten.model.generated.ExpandPartij;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class KlantClientService {
    private KlantClient klantClient;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public KlantClientService() {
    }

    @Inject
    public KlantClientService(@RestClient KlantClient klantClient) {
        this.klantClient = klantClient;
    }

    public List<DigitaalAdres> findDigitalAddressesByNumber(final String number) {
        ExpandPartij party = convertToSingleItem(
                klantClient.partijenList(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "digitaleAdressen",
                        null,
                        null,
                        null,
                        1,
                        null,
                        null,
                        null,
                        number,
                        null,
                        null,
                        null
                ).getResults()
        );
        if (party == null || party.getExpand() == null) {
            return Collections.emptyList();
        }
        return party.getExpand().getDigitaleAdressen();
    }

    public List<Betrokkene> listBetrokkenenByNumber(final String number, final Integer page) {
        ExpandPartij party = convertToSingleItem(
                klantClient.partijenList(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "betrokkenen,betrokkenen.hadKlantcontact",
                        null,
                        null,
                        null,
                        page,
                        null,
                        null,
                        null,
                        number,
                        null,
                        null,
                        null
                ).getResults()
        );
        if (party == null || party.getExpand() == null) {
            return Collections.emptyList();
        }
        return party.getExpand().getBetrokkenen();
    }

    private <T> T convertToSingleItem(final List<T> list) {
        return switch (list.size()) {
            case 0 -> null;
            case 1 -> list.getFirst();
            default -> throw new IllegalStateException("Too many results: %d".formatted(list.size()));
        };
    }
}
