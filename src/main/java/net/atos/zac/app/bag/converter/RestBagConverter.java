/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;

import static nl.info.client.zgw.util.UriUtilsKt.extractUuid;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats;
import net.atos.zac.app.bag.model.RESTBAGAdres;
import net.atos.zac.app.bag.model.RESTBAGObject;
import net.atos.zac.app.bag.model.RESTBAGObjectGegevens;
import net.atos.zac.app.bag.model.RESTNummeraanduiding;
import net.atos.zac.app.bag.model.RESTOpenbareRuimte;
import net.atos.zac.app.bag.model.RESTPand;
import net.atos.zac.app.bag.model.RESTWoonplaats;
import nl.info.client.bag.model.generated.PointGeoJSON;
import nl.info.client.bag.model.generated.PuntOfVlak;
import nl.info.client.bag.model.generated.Surface;
import nl.info.zac.app.zaak.model.RestCoordinates;
import nl.info.zac.app.zaak.model.RestGeometry;

public class RestBagConverter {
    public static Zaakobject convertToZaakobject(final RESTBAGObject restbagObject, final Zaak zaak) {
        return switch (restbagObject.getBagObjectType()) {
            case ADRES -> RestAdresConverter.convertToZaakobject((RESTBAGAdres) restbagObject, zaak);
            case PAND -> RestPandConverter.convertToZaakobject((RESTPand) restbagObject, zaak);
            case WOONPLAATS -> RestWoonplaatsConverter.convertToZaakobject((RESTWoonplaats) restbagObject, zaak);
            case OPENBARE_RUIMTE -> RestOpenbareRuimteConverter.convertToZaakobject((RESTOpenbareRuimte) restbagObject, zaak);
            case NUMMERAANDUIDING -> RestNummeraanduidingConverter.convertToZaakobject((RESTNummeraanduiding) restbagObject, zaak);
            case ADRESSEERBAAR_OBJECT -> throw new NotImplementedException();
        };
    }

    public static RESTBAGObject convertToRESTBAGObject(final Zaakobject zaakobject) {
        return switch (zaakobject.getObjectType()) {
            case ADRES -> RestAdresConverter.convertToREST((ZaakobjectAdres) zaakobject);
            case PAND -> RestPandConverter.convertToREST((ZaakobjectPand) zaakobject);
            case WOONPLAATS -> RestWoonplaatsConverter.convertToREST((ZaakobjectWoonplaats) zaakobject);
            case OPENBARE_RUIMTE -> RestOpenbareRuimteConverter.convertToREST((ZaakobjectOpenbareRuimte) zaakobject);
            case OVERIGE -> RestNummeraanduidingConverter.convertToREST((ZaakobjectNummeraanduiding) zaakobject); // voor nu alleen nummeraanduiding
            default -> throw new IllegalStateException("Unexpected objectType: " + zaakobject.getObjectType());
        };
    }

    public static RESTBAGObjectGegevens convertToRESTBAGObjectGegevens(final Zaakobject zaakobject) {
        final RESTBAGObjectGegevens restZaakobject = new RESTBAGObjectGegevens();
        restZaakobject.zaakobject = convertToRESTBAGObject(zaakobject);
        restZaakobject.uuid = zaakobject.getUuid();
        restZaakobject.zaakUuid = extractUuid(zaakobject.getZaak());
        return restZaakobject;
    }

    public static String getHuisnummerWeergave(final Integer huisnummer, final String huisletter, final String huisnummertoevoeging) {
        final StringBuilder volledigHuisnummer = new StringBuilder();
        volledigHuisnummer.append(huisnummer);

        if (StringUtils.isNotBlank(huisletter)) {
            volledigHuisnummer.append(huisletter);
        }
        if (StringUtils.isNotBlank(huisnummertoevoeging)) {
            volledigHuisnummer.append("-").append(huisnummertoevoeging);
        }
        return volledigHuisnummer.toString().trim();
    }

    public static RestGeometry convertVlak(final Surface surface) {
        return new RestGeometry(
                surface.getType().value(),
                null,
                surface.getCoordinates()
                        .stream()
                        .map(coords -> coords.stream()
                                .map(RestBagConverter::convertCoordinates)
                                .toList())
                        .toList(),
                null
        );
    }

    public static RestGeometry convertPunt(PointGeoJSON punt) {
        return new RestGeometry(
                punt.getType().value(),
                convertCoordinates(punt.getCoordinates()),
                null,
                null);
    }

    public static RestGeometry convertPuntOrVlak(PuntOfVlak puntOfVlak) {
        if (puntOfVlak.getPunt() != null) {
            return convertPunt(puntOfVlak.getPunt());
        } else if (puntOfVlak.getVlak() != null) {
            return convertVlak(puntOfVlak.getVlak());
        }
        return null;
    }

    public static RestCoordinates convertCoordinates(List<BigDecimal> coordinates) {
        return new RestCoordinates(coordinates.get(1).doubleValue(), coordinates.get(0).doubleValue());
    }
}
