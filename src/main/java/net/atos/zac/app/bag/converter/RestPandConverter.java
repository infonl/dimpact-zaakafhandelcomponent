/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;

import java.net.URI;
import java.util.List;

import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectPand;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand;
import net.atos.zac.app.bag.model.RESTPand;
import nl.info.client.bag.model.generated.Indicatie;
import nl.info.client.bag.model.generated.Pand;
import nl.info.client.bag.model.generated.PandIOHal;
import nl.info.client.bag.model.generated.PandIOHalBasis;

public class RestPandConverter {
    public static List<RESTPand> convertToREST(final List<PandIOHalBasis> panden) {
        if (panden == null) {
            return List.of();
        }
        return panden.stream().map(RestPandConverter::convertToREST).toList();
    }

    public static RESTPand convertToREST(final PandIOHalBasis pandIO) {
        if (pandIO == null) {
            return null;
        }
        return convertToREST(pandIO.getPand());
    }

    public static RESTPand convertToREST(final PandIOHal pandIO) {
        if (pandIO == null) {
            return null;
        }
        final RESTPand restPand = convertToREST(pandIO.getPand());
        restPand.url = URI.create(pandIO.getLinks().getSelf().getHref());
        return restPand;
    }

    public static RESTPand convertToREST(final ZaakobjectPand zaakobjectPand) {
        if (zaakobjectPand == null || zaakobjectPand.getObjectIdentificatie() == null) {
            return null;
        }
        final ObjectPand pand = zaakobjectPand.getObjectIdentificatie();
        final RESTPand restPand = new RESTPand();
        restPand.identificatie = pand.getIdentificatie();
        return restPand;
    }

    public static ZaakobjectPand convertToZaakobject(final RESTPand pand, final Zaak zaak) {
        return new ZaakobjectPand(zaak.getUrl(), pand.url, new ObjectPand(pand.identificatie));
    }

    public static RESTPand convertToREST(final Pand pand) {
        final RESTPand restPand = new RESTPand();
        restPand.identificatie = pand.getIdentificatie();
        restPand.status = pand.getStatus();
        if (pand.getStatus() != null) {
            restPand.statusWeergave = pand.getStatus().toString();
        }
        restPand.oorspronkelijkBouwjaar = pand.getOorspronkelijkBouwjaar();
        restPand.geconstateerd = Indicatie.J.equals(pand.getGeconstateerd());
        restPand.geometry = RestBagConverter.convertVlak(pand.getGeometrie());
        return restPand;
    }

}
