/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.productaanvragen.converter;

import java.util.List;
import java.util.stream.Collectors;

import net.atos.zac.app.productaanvragen.model.RESTInboxProductaanvraag;
import net.atos.zac.productaanvraag.model.InboxProductaanvraag;

public class RESTInboxProductaanvraagConverter {

    public static RESTInboxProductaanvraag convert(final InboxProductaanvraag productaanvraag) {
        final RESTInboxProductaanvraag restInboxProductaanvraag = new RESTInboxProductaanvraag();
        restInboxProductaanvraag.id = productaanvraag.getId();
        restInboxProductaanvraag.aanvraagdocumentUUID = productaanvraag.getAanvraagdocumentUUID();
        restInboxProductaanvraag.productaanvraagObjectUUID = productaanvraag.getProductaanvraagObjectUUID();
        restInboxProductaanvraag.initiatorID = productaanvraag.getInitiatorID();
        restInboxProductaanvraag.aantalBijlagen = productaanvraag.getAantalBijlagen();
        restInboxProductaanvraag.type = productaanvraag.getType();
        restInboxProductaanvraag.ontvangstdatum = productaanvraag.getOntvangstdatum();
        return restInboxProductaanvraag;
    }

    public static List<RESTInboxProductaanvraag> convert(final List<InboxProductaanvraag> productaanvragen) {
        return productaanvragen.stream().map(RESTInboxProductaanvraagConverter::convert).collect(Collectors.toList());
    }
}
