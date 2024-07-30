/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klanten;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import net.atos.client.klanten.model.Betrokkene;
import net.atos.client.klanten.model.ExpandKlantcontact;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Singleton
public class KlantClientService {

    @Inject
    @RestClient
    private KlantClient klantenClient;

    public ExpandKlantcontact findKlantcontactByNummer(final String nummer) {
        Betrokkene betrokkene = convertToSingleItem(klantenClient.betrokkenenList(
            null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, nummer, null, null
        ).getResults());
        return getKlantContactByBetrokkene(betrokkene);
    }

    private <T> T convertToSingleItem(final List<T> list) {
        return switch (list.size()) {
            case 0 -> null;
            case 1 -> list.getFirst();
            default -> throw new IllegalStateException("Too many results: %d".formatted(list.size()));
        };
    }

    private ExpandKlantcontact getKlantContactByBetrokkene(final Betrokkene betrokkene) {
        if (betrokkene == null || betrokkene.getHadKlantcontact() == null) {
            return null;
        }
        return klantenClient.klantcontactenRetrieve(betrokkene.getHadKlantcontact().getUuid(), null);
    }
}
