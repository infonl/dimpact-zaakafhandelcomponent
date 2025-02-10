/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;

import java.net.URI;

import net.atos.client.bag.model.generated.AdresIOHal;
import net.atos.client.bag.model.generated.Indicatie;
import net.atos.client.bag.model.generated.OpenbareRuimte;
import net.atos.client.bag.model.generated.OpenbareRuimteIOHal;
import net.atos.client.bag.model.generated.OpenbareRuimteIOHalBasis;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte;
import net.atos.zac.app.bag.model.RESTOpenbareRuimte;

public class RestOpenbareRuimteConverter {
    public static RESTOpenbareRuimte convertToREST(final OpenbareRuimteIOHalBasis openbareRuimteIO, final AdresIOHal adres) {
        if (openbareRuimteIO == null) {
            return null;
        }
        final RESTOpenbareRuimte restOpenbareRuimte = convertToREST(openbareRuimteIO);
        restOpenbareRuimte.woonplaatsNaam = adres != null ? adres.getWoonplaatsNaam() : openbareRuimteIO.getOpenbareRuimte().getLigtIn();
        return restOpenbareRuimte;
    }

    public static RESTOpenbareRuimte convertToREST(final OpenbareRuimteIOHalBasis openbareRuimteIO) {
        if (openbareRuimteIO == null) {
            return null;
        }
        final RESTOpenbareRuimte restOpenbareRuimte = convertToREST(openbareRuimteIO.getOpenbareRuimte());
        restOpenbareRuimte.url = URI.create(openbareRuimteIO.getLinks().getSelf().getHref());
        return restOpenbareRuimte;
    }

    public static RESTOpenbareRuimte convertToREST(final OpenbareRuimteIOHal openbareRuimteIO) {
        if (openbareRuimteIO == null) {
            return null;
        }
        final RESTOpenbareRuimte restOpenbareRuimte = convertToREST(openbareRuimteIO.getOpenbareRuimte());
        restOpenbareRuimte.url = URI.create(openbareRuimteIO.getLinks().getSelf().getHref());
        if (openbareRuimteIO.getEmbedded() != null) {
            restOpenbareRuimte.woonplaats = RestWoonplaatsConverter.convertToREST(openbareRuimteIO.getEmbedded().getLigtInWoonplaats());
        }
        return restOpenbareRuimte;
    }

    public static RESTOpenbareRuimte convertToREST(final ZaakobjectOpenbareRuimte zaakobjectOpenbareRuimte) {
        if (zaakobjectOpenbareRuimte == null || zaakobjectOpenbareRuimte.getObjectIdentificatie() == null) {
            return null;
        }
        final ObjectOpenbareRuimte openbareRuimte = zaakobjectOpenbareRuimte.getObjectIdentificatie();
        final RESTOpenbareRuimte restOpenbareRuimte = new RESTOpenbareRuimte();
        restOpenbareRuimte.url = zaakobjectOpenbareRuimte.getObject();
        restOpenbareRuimte.identificatie = openbareRuimte.getIdentificatie();
        restOpenbareRuimte.naam = openbareRuimte.getGorOpenbareRuimteNaam();
        restOpenbareRuimte.woonplaatsNaam = openbareRuimte.getWplWoonplaatsNaam();
        return restOpenbareRuimte;
    }

    public static ZaakobjectOpenbareRuimte convertToZaakobject(final RESTOpenbareRuimte openbareRuimte, final Zaak zaak) {
        final ObjectOpenbareRuimte objectOpenbareRuimte = new ObjectOpenbareRuimte(
                openbareRuimte.identificatie,
                openbareRuimte.naam,
                openbareRuimte.woonplaatsNaam);
        return new ZaakobjectOpenbareRuimte(zaak.getUrl(), openbareRuimte.url, objectOpenbareRuimte);
    }

    private static RESTOpenbareRuimte convertToREST(final OpenbareRuimte openbareRuimte) {
        final RESTOpenbareRuimte restOpenbareRuimte = new RESTOpenbareRuimte();
        restOpenbareRuimte.identificatie = openbareRuimte.getIdentificatie();
        restOpenbareRuimte.naam = openbareRuimte.getNaam();
        restOpenbareRuimte.woonplaatsNaam = openbareRuimte.getLigtIn();
        restOpenbareRuimte.status = openbareRuimte.getStatus();
        restOpenbareRuimte.type = openbareRuimte.getType();
        if (openbareRuimte.getType() != null) {
            restOpenbareRuimte.typeWeergave = openbareRuimte.getType().toString();
        }
        restOpenbareRuimte.geconstateerd = Indicatie.J.equals(openbareRuimte.getGeconstateerd());
        return restOpenbareRuimte;
    }

}
