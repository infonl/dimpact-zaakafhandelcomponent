/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.contactmomenten;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import net.atos.client.contactmomenten.exception.NotFoundException;
import net.atos.client.contactmomenten.model.KlantcontactmomentListParameters;
import net.atos.client.contactmomenten.model.generated.ContactMoment;
import net.atos.client.contactmomenten.model.generated.KlantcontactmomentList200Response;

@Singleton
public class ContactmomentenClientService {
    private KlantcontactmomentenClient klantcontactmomentenClient;
    private ContactmomentenClient contactmomentenClient;

    /**
     * Empty no-op constructor as required by Weld.
     */
    public ContactmomentenClientService() {
    }

    @Inject
    public ContactmomentenClientService(
            final @RestClient KlantcontactmomentenClient klantcontactmomentenClient,
            final @RestClient ContactmomentenClient contactmomentenClient
    ) {
        this.klantcontactmomentenClient = klantcontactmomentenClient;
        this.contactmomentenClient = contactmomentenClient;
    }

    public KlantcontactmomentList200Response listKlantcontactmomenten(
            final KlantcontactmomentListParameters parameters
    ) {
        try {
            return klantcontactmomentenClient.klantcontactmomentList(parameters);
        } catch (final NotFoundException exception) {
            return new KlantcontactmomentList200Response().count(0);
        }
    }

    public ContactMoment readContactmoment(final UUID uuid) {
        return contactmomentenClient.contactmomentRead(uuid, StringUtils.EMPTY);
    }
}
