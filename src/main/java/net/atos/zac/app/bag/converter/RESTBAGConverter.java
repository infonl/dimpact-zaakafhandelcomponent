/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.converter;

import java.math.BigDecimal;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import net.atos.client.bag.model.generated.PointGeoJSON;
import net.atos.client.bag.model.generated.PuntOfVlak;
import net.atos.client.bag.model.generated.Surface;
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
import net.atos.zac.app.zaak.model.RestCoordinates;
import net.atos.zac.app.zaak.model.RestGeometry;
import net.atos.zac.util.UriUtil;

public class RESTBAGConverter {

    @Inject
    private RESTAdresConverter adresConverter;

    @Inject
    private RESTNummeraanduidingConverter nummeraanduidingConverter;

    @Inject
    private RESTOpenbareRuimteConverter openbareRuimteConverter;

    @Inject
    private RESTPandConverter pandConverter;

    @Inject
    private RESTWoonplaatsConverter woonplaatsConverter;

    public Zaakobject convertToZaakobject(final RESTBAGObject restbagObject, final Zaak zaak) {
        return switch (restbagObject.getBagObjectType()) {
            case ADRES -> adresConverter.convertToZaakobject((RESTBAGAdres) restbagObject, zaak);
            case PAND -> pandConverter.convertToZaakobject((RESTPand) restbagObject, zaak);
            case WOONPLAATS -> woonplaatsConverter.convertToZaakobject((RESTWoonplaats) restbagObject, zaak);
            case OPENBARE_RUIMTE -> openbareRuimteConverter.convertToZaakobject((RESTOpenbareRuimte) restbagObject, zaak);
            case NUMMERAANDUIDING -> nummeraanduidingConverter.convertToZaakobject((RESTNummeraanduiding) restbagObject, zaak);
            case ADRESSEERBAAR_OBJECT -> throw new NotImplementedException();
        };
    }

    public RESTBAGObject convertToRESTBAGObject(final Zaakobject zaakobject) {
        return switch (zaakobject.getObjectType()) {
            case ADRES -> adresConverter.convertToREST((ZaakobjectAdres) zaakobject);
            case PAND -> pandConverter.convertToREST((ZaakobjectPand) zaakobject);
            case WOONPLAATS -> woonplaatsConverter.convertToREST((ZaakobjectWoonplaats) zaakobject);
            case OPENBARE_RUIMTE -> openbareRuimteConverter.convertToREST((ZaakobjectOpenbareRuimte) zaakobject);
            case OVERIGE -> nummeraanduidingConverter.convertToREST((ZaakobjectNummeraanduiding) zaakobject); // voor nu alleen nummeraanduiding
            default -> throw new IllegalStateException("Unexpected objectType: " + zaakobject.getObjectType());
        };
    }

    public RESTBAGObjectGegevens convertToRESTBAGObjectGegevens(final Zaakobject zaakobject) {
        final RESTBAGObjectGegevens restZaakobject = new RESTBAGObjectGegevens();
        restZaakobject.zaakobject = convertToRESTBAGObject(zaakobject);
        restZaakobject.uuid = zaakobject.getUuid();
        restZaakobject.zaakUuid = UriUtil.uuidFromURI(zaakobject.getZaak());
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
                                .map(RESTBAGConverter::convertCoordinates)
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
