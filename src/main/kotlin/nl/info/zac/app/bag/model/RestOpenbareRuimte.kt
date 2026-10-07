/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.bag.model;

import nl.info.client.bag.model.generated.StatusNaamgeving;
import nl.info.client.bag.model.generated.TypeOpenbareRuimte;

public class RESTOpenbareRuimte extends RESTBAGObject {
    public String naam;

    public TypeOpenbareRuimte type;

    public String typeWeergave;

    public StatusNaamgeving status;

    public String woonplaatsNaam;

    public RESTWoonplaats woonplaats;

    public RESTOpenbareRuimte() {
    }

    @Override
    public BAGObjectType getBagObjectType() {
        return BAGObjectType.OPENBARE_RUIMTE;
    }

    @Override
    public String getOmschrijving() {
        return naam;
    }
}
