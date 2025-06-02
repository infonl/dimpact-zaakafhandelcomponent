/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;


import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.Zaak;
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectNummeraanduiding;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding;
import net.atos.zac.app.bag.model.RESTNummeraanduiding;
import nl.info.client.bag.model.generated.Indicatie;
import nl.info.client.bag.model.generated.Nummeraanduiding;
import nl.info.client.bag.model.generated.NummeraanduidingIOHal;
import nl.info.client.bag.model.generated.NummeraanduidingIOHalBasis;
import nl.info.client.bag.model.generated.StatusNaamgeving;
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject;

public class RestNummeraanduidingConverter {
    public static RESTNummeraanduiding convertToREST(final NummeraanduidingIOHalBasis nummeraanduidingIO) {
        if (nummeraanduidingIO == null) {
            return null;
        }
        final RESTNummeraanduiding restNummeraanduiding = convertToREST(nummeraanduidingIO.getNummeraanduiding());
        restNummeraanduiding.url = URI.create(nummeraanduidingIO.getLinks().getSelf().getHref());
        return restNummeraanduiding;
    }

    public static RESTNummeraanduiding convertToREST(final NummeraanduidingIOHal nummeraanduidingIO) {
        if (nummeraanduidingIO == null) {
            return null;
        }
        final RESTNummeraanduiding restNummeraanduiding = convertToREST(nummeraanduidingIO.getNummeraanduiding());
        restNummeraanduiding.url = URI.create(nummeraanduidingIO.getLinks().getSelf().getHref());
        if (nummeraanduidingIO.getEmbedded() != null) {
            restNummeraanduiding.woonplaats = RestWoonplaatsConverter.convertToREST(nummeraanduidingIO.getEmbedded().getLigtInWoonplaats());
            restNummeraanduiding.openbareRuimte = RestOpenbareRuimteConverter.convertToREST(nummeraanduidingIO.getEmbedded()
                    .getLigtAanOpenbareRuimte());
        }
        return restNummeraanduiding;
    }

    public static RESTNummeraanduiding convertToREST(final ZaakobjectNummeraanduiding zaakobjectNummeraanduiding) {
        if (zaakobjectNummeraanduiding == null || zaakobjectNummeraanduiding.getObjectIdentificatie() == null) {
            return null;
        }
        final ObjectNummeraanduiding nummeraanduiding = zaakobjectNummeraanduiding.getObjectIdentificatie().overigeData;
        final RESTNummeraanduiding restNummeraanduiding = new RESTNummeraanduiding();
        restNummeraanduiding.url = zaakobjectNummeraanduiding.getObject();
        restNummeraanduiding.identificatie = nummeraanduiding.getIdentificatie();
        restNummeraanduiding.postcode = nummeraanduiding.getPostcode();
        restNummeraanduiding.huisnummer = nummeraanduiding.getHuisnummer();
        restNummeraanduiding.huisletter = nummeraanduiding.getHuisletter();
        restNummeraanduiding.huisnummertoevoeging = nummeraanduiding.getHuisnummertoevoeging();
        restNummeraanduiding.huisnummerWeergave = convertHuisnummerWeergave(nummeraanduiding);
        restNummeraanduiding.status = StatusNaamgeving.fromValue(nummeraanduiding.getStatus());
        restNummeraanduiding.typeAdresseerbaarObject = TypeAdresseerbaarObject.fromValue(nummeraanduiding.getTypeAdresseerbaarObject());
        return restNummeraanduiding;
    }

    public static ZaakobjectNummeraanduiding convertToZaakobject(final RESTNummeraanduiding nummeraanduiding, final Zaak zaak) {
        final ObjectNummeraanduiding objectNummeraanduiding = new ObjectNummeraanduiding(
                nummeraanduiding.identificatie,
                nummeraanduiding.huisnummer,
                nummeraanduiding.huisletter,
                nummeraanduiding.huisnummertoevoeging,
                nummeraanduiding.postcode,
                nummeraanduiding.typeAdresseerbaarObject != null ? nummeraanduiding.typeAdresseerbaarObject.toString() : null,
                nummeraanduiding.status != null ? nummeraanduiding.status.toString() : null
        );
        return new ZaakobjectNummeraanduiding(zaak.getUrl(), nummeraanduiding.url, objectNummeraanduiding);
    }

    private static String convertHuisnummerWeergave(final Nummeraanduiding nummeraanduiding) {
        return RestBagConverter.getHuisnummerWeergave(nummeraanduiding.getHuisnummer(), nummeraanduiding.getHuisletter(),
                nummeraanduiding.getHuisnummertoevoeging());
    }

    private static String convertHuisnummerWeergave(final ObjectNummeraanduiding nummeraanduiding) {
        return RestBagConverter.getHuisnummerWeergave(nummeraanduiding.getHuisnummer(), nummeraanduiding.getHuisletter(),
                nummeraanduiding.getHuisnummertoevoeging());
    }

    public static RESTNummeraanduiding convertToREST(final Nummeraanduiding nummeraanduiding) {
        final RESTNummeraanduiding restNummeraanduiding = new RESTNummeraanduiding();
        restNummeraanduiding.identificatie = nummeraanduiding.getIdentificatie();
        restNummeraanduiding.postcode = nummeraanduiding.getPostcode();
        restNummeraanduiding.huisnummer = nummeraanduiding.getHuisnummer();
        restNummeraanduiding.huisletter = nummeraanduiding.getHuisletter();
        restNummeraanduiding.huisnummertoevoeging = nummeraanduiding.getHuisnummertoevoeging();
        restNummeraanduiding.huisnummerWeergave = convertHuisnummerWeergave(nummeraanduiding);
        restNummeraanduiding.status = nummeraanduiding.getStatus();
        restNummeraanduiding.typeAdresseerbaarObject = nummeraanduiding.getTypeAdresseerbaarObject();
        restNummeraanduiding.geconstateerd = Indicatie.J.equals(nummeraanduiding.getGeconstateerd());
        return restNummeraanduiding;
    }
}
