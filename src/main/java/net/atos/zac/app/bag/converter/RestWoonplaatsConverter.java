/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.converter;

import java.net.URI;

import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectWoonplaats;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats;
import net.atos.zac.app.bag.model.RESTWoonplaats;
import nl.info.client.bag.model.generated.Indicatie;
import nl.info.client.bag.model.generated.Woonplaats;
import nl.info.client.bag.model.generated.WoonplaatsIOHal;
import nl.info.client.bag.model.generated.WoonplaatsIOHalBasis;
import nl.info.client.zgw.zrc.model.generated.Zaak;

public class RestWoonplaatsConverter {
    public static RESTWoonplaats convertToREST(final WoonplaatsIOHalBasis woonplaatsIO) {
        if (woonplaatsIO == null) {
            return null;
        }
        final RESTWoonplaats restWoonplaats = convertToREST(woonplaatsIO.getWoonplaats());
        restWoonplaats.url = URI.create(woonplaatsIO.getLinks().getSelf().getHref());
        return restWoonplaats;
    }

    public static RESTWoonplaats convertToREST(final WoonplaatsIOHal woonplaatsIO) {
        if (woonplaatsIO == null) {
            return null;
        }
        final RESTWoonplaats restWoonplaats = convertToREST(woonplaatsIO.getWoonplaats());
        restWoonplaats.url = URI.create(woonplaatsIO.getLinks().getSelf().getHref());
        return restWoonplaats;
    }

    public static RESTWoonplaats convertToREST(final ZaakobjectWoonplaats zaakobjectWoonplaats) {
        if (zaakobjectWoonplaats == null || zaakobjectWoonplaats.getObjectIdentificatie() == null) {
            return null;
        }
        final ObjectWoonplaats woonplaats = zaakobjectWoonplaats.getObjectIdentificatie();
        final RESTWoonplaats restWoonplaats = new RESTWoonplaats();
        restWoonplaats.url = zaakobjectWoonplaats.getObject();
        restWoonplaats.identificatie = woonplaats.getIdentificatie();
        restWoonplaats.naam = woonplaats.getWoonplaatsNaam();
        return restWoonplaats;
    }

    public static ZaakobjectWoonplaats convertToZaakobject(final RESTWoonplaats woonplaats, final Zaak zaak) {
        return new ZaakobjectWoonplaats(zaak.getUrl(), woonplaats.url, new ObjectWoonplaats(woonplaats.identificatie, woonplaats.naam));
    }

    private static RESTWoonplaats convertToREST(final Woonplaats woonplaats) {
        final RESTWoonplaats restWoonplaats = new RESTWoonplaats();
        restWoonplaats.identificatie = woonplaats.getIdentificatie();
        restWoonplaats.naam = woonplaats.getNaam();
        restWoonplaats.status = woonplaats.getStatus();
        restWoonplaats.geconstateerd = Indicatie.J.equals(woonplaats.getGeconstateerd());
        return restWoonplaats;
    }
}
