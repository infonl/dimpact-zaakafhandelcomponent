/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.model;

import nl.info.client.bag.model.generated.StatusNaamgeving;
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject;

public class RESTNummeraanduiding extends RESTBAGObject {

    public String huisnummerWeergave;

    public int huisnummer;

    public String huisletter;

    public String huisnummertoevoeging;

    public String postcode;

    public TypeAdresseerbaarObject typeAdresseerbaarObject;

    public StatusNaamgeving status;

    public RESTWoonplaats woonplaats;

    public RESTOpenbareRuimte openbareRuimte;

    public RESTNummeraanduiding() {
    }

    @Override
    public BAGObjectType getBagObjectType() {
        return BAGObjectType.NUMMERAANDUIDING;
    }

    @Override
    public String getOmschrijving() {
        return "%s %s".formatted(huisnummerWeergave, postcode);
    }

}
