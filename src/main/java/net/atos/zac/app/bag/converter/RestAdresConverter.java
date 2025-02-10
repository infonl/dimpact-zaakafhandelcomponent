/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;

import java.net.URI;

import org.apache.commons.lang3.BooleanUtils;

import net.atos.client.bag.model.generated.AdresIOHal;
import net.atos.client.bag.model.generated.Geconstateerd;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectAdres;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres;
import net.atos.zac.app.bag.model.RESTBAGAdres;

public class RestAdresConverter {
    public static RESTBAGAdres convertToREST(final AdresIOHal adres) {
        if (adres == null) {
            return null;
        }
        final RESTBAGAdres restBAGAdres = new RESTBAGAdres();
        restBAGAdres.url = URI.create(adres.getLinks().getSelf().getHref());
        restBAGAdres.identificatie = adres.getNummeraanduidingIdentificatie();
        restBAGAdres.postcode = adres.getPostcode();
        restBAGAdres.huisnummer = adres.getHuisnummer();
        restBAGAdres.huisletter = adres.getHuisletter();
        restBAGAdres.huisnummertoevoeging = adres.getHuisnummertoevoeging();
        restBAGAdres.huisnummerWeergave = convertToVolledigHuisnummer(adres);
        restBAGAdres.openbareRuimteNaam = adres.getOpenbareRuimteNaam();
        restBAGAdres.woonplaatsNaam = adres.getWoonplaatsNaam();
        if (adres.getGeconstateerd() != null) {
            final Geconstateerd geconstateerd = adres.getGeconstateerd();
            restBAGAdres.geconstateerd = BooleanUtils.isTrue(geconstateerd.getNummeraanduiding()) &&
                                         BooleanUtils.isTrue(geconstateerd.getWoonplaats()) &&
                                         BooleanUtils.isTrue(geconstateerd.getOpenbareRuimte());
        }

        if (adres.getEmbedded() != null) {
            restBAGAdres.openbareRuimte = RestOpenbareRuimteConverter.convertToREST(adres.getEmbedded().getOpenbareRuimte(), adres);
            restBAGAdres.nummeraanduiding = RestNummeraanduidingConverter.convertToREST(adres.getEmbedded().getNummeraanduiding());
            restBAGAdres.woonplaats = RestWoonplaatsConverter.convertToREST(adres.getEmbedded().getWoonplaats());
            restBAGAdres.panden = RestPandConverter.convertToREST(adres.getEmbedded().getPanden());
            restBAGAdres.adresseerbaarObject = RestAdresseerbaarObjectConverter.convertToREST(adres.getEmbedded().getAdresseerbaarObject());
        }
        return restBAGAdres;
    }


    public static RESTBAGAdres convertToREST(final ZaakobjectAdres zaakobjectAdres) {
        if (zaakobjectAdres == null || zaakobjectAdres.getObjectIdentificatie() == null) {
            return null;
        }
        final ObjectAdres adres = zaakobjectAdres.getObjectIdentificatie();
        final RESTBAGAdres restBAGAdres = new RESTBAGAdres();
        restBAGAdres.url = zaakobjectAdres.getObject();
        restBAGAdres.identificatie = adres.getIdentificatie();
        restBAGAdres.postcode = adres.getPostcode();
        restBAGAdres.huisnummerWeergave = convertToVolledigHuisnummer(adres);
        restBAGAdres.openbareRuimteNaam = adres.getGorOpenbareRuimteNaam();
        restBAGAdres.woonplaatsNaam = adres.getWplWoonplaatsNaam();
        return restBAGAdres;
    }

    public static ZaakobjectAdres convertToZaakobject(final RESTBAGAdres restBAGAdres, final Zaak zaak) {
        ObjectAdres objectAdres = new ObjectAdres(
                restBAGAdres.identificatie,
                restBAGAdres.woonplaatsNaam,
                restBAGAdres.openbareRuimteNaam,
                restBAGAdres.huisnummer,
                restBAGAdres.huisletter,
                restBAGAdres.huisnummertoevoeging,
                restBAGAdres.postcode
        );
        return new ZaakobjectAdres(zaak.getUrl(), restBAGAdres.url, objectAdres);
    }

    private static String convertToVolledigHuisnummer(final AdresIOHal adresHal) {
        return RestBagConverter.getHuisnummerWeergave(adresHal.getHuisnummer(), adresHal.getHuisletter(), adresHal
                .getHuisnummertoevoeging());
    }

    private static String convertToVolledigHuisnummer(final ObjectAdres objectAdres) {
        return RestBagConverter.getHuisnummerWeergave(objectAdres.getHuisnummer(), objectAdres.getHuisletter(), objectAdres
                .getHuisnummertoevoeging());
    }
}
