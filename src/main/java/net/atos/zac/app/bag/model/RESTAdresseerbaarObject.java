/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.bag.model;

import net.atos.client.bag.model.TypeAdresseerbaarObject;
import net.atos.zac.app.zaken.model.RESTGeometry;

public class RESTAdresseerbaarObject extends RESTBAGObject {

    public TypeAdresseerbaarObject typeAdresseerbaarObject;

    public String status;

    public String vboDoel;

    public int vboOppervlakte;

    public RESTGeometry geometry;

    public RESTAdresseerbaarObject() {}

    @Override
    public BAGObjectType getBagObjectType() {
        return BAGObjectType.ADRESSEERBAAR_OBJECT;
    }

    @Override
    public String getOmschrijving() {
        return "%s %s".formatted(typeAdresseerbaarObject.toString(), identificatie);
    }
}
