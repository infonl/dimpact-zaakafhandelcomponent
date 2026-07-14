/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import net.atos.zac.app.bag.model.BAGObjectType;
import net.atos.zac.app.bag.model.RESTAdresseerbaarObject;
import net.atos.zac.app.bag.model.RESTBAGAdres;
import net.atos.zac.app.bag.model.RESTBAGObject;
import net.atos.zac.app.bag.model.RESTNummeraanduiding;
import net.atos.zac.app.bag.model.RESTOpenbareRuimte;
import net.atos.zac.app.bag.model.RESTPand;
import net.atos.zac.app.bag.model.RESTWoonplaats;
import nl.info.client.zgw.util.JsonbUtilKt;

public class RESTBAGObjectJsonbDeserializer implements JsonbDeserializer<RESTBAGObject> {

    @Override
    public RESTBAGObject deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
        final JsonObject jsonObject = parser.getObject();
        final BAGObjectType type = BAGObjectType.valueOf(jsonObject.getJsonString("bagObjectType").getString());
        final Jsonb jsonb = JsonbUtilKt.getJSONB();

        return switch (type) {
            case ADRES -> jsonb.fromJson(jsonObject.toString(), RESTBAGAdres.class);
            case NUMMERAANDUIDING -> jsonb.fromJson(jsonObject.toString(), RESTNummeraanduiding.class);
            case WOONPLAATS -> jsonb.fromJson(jsonObject.toString(), RESTWoonplaats.class);
            case PAND -> jsonb.fromJson(jsonObject.toString(), RESTPand.class);
            case OPENBARE_RUIMTE -> jsonb.fromJson(jsonObject.toString(), RESTOpenbareRuimte.class);
            case ADRESSEERBAAR_OBJECT -> jsonb.fromJson(jsonObject.toString(), RESTAdresseerbaarObject.class);
        };
    }
}
