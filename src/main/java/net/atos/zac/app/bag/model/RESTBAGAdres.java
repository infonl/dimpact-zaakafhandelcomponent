/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.model;

import java.util.ArrayList;
import java.util.List;

import net.atos.zac.app.zaak.model.RestGeometry;

public class RESTBAGAdres extends RESTBAGObject {

    public String postcode;

    public String huisnummerWeergave;

    public int huisnummer;

    public String huisletter;

    public String huisnummertoevoeging;

    public String openbareRuimteNaam;

    public String woonplaatsNaam;

    public RESTOpenbareRuimte openbareRuimte;

    public RESTNummeraanduiding nummeraanduiding;

    public RESTWoonplaats woonplaats;

    public RESTAdresseerbaarObject adresseerbaarObject;

    public List<RESTPand> panden = new ArrayList<>();

    public RESTBAGAdres() {
    }

    @Override
    public BAGObjectType getBagObjectType() {
        return BAGObjectType.ADRES;
    }

    @Override
    public String getOmschrijving() {
        return "%s %s, %s %s".formatted(openbareRuimteNaam, huisnummerWeergave, postcode, woonplaatsNaam);
    }

    public RestGeometry getGeometry() {
        List<RestGeometry> restGeometries = new ArrayList<>();
        if (adresseerbaarObject != null && adresseerbaarObject.geometry != null) {
            restGeometries.add(adresseerbaarObject.geometry);
        }
        if (panden != null && !panden.isEmpty() && panden.getFirst().geometry != null) {
            restGeometries.add(panden.getFirst().geometry);
        }
        RestGeometry restGeometry = new RestGeometry(
                "GeometryCollection",
                null,
                null,
                restGeometries
        );

        if (restGeometries.size() == 1) {
            return restGeometries.getFirst();
        }
        if (restGeometries.size() == 2) {
            return restGeometry;
        }
        return null;
    }
}
