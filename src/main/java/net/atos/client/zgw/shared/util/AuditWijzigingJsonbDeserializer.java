/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import static net.atos.client.zgw.shared.util.JsonbUtil.JSONB;

import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.model.audit.AuditWijziging;

public class AuditWijzigingJsonbDeserializer implements JsonbDeserializer<AuditWijziging<?>> {

    @Override
    public AuditWijziging<?> deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
        final JsonObject wijzigingObject = parser.getObject();
        final JsonObject waardeObject;
        if (!wijzigingObject.isNull("oud")) {
            waardeObject = wijzigingObject.get("oud").asJsonObject();
        } else if (!wijzigingObject.isNull("nieuw")) {
            waardeObject = wijzigingObject.get("nieuw").asJsonObject();
        } else {
            return null;
        }

        final ObjectType type = ObjectType.getObjectType(waardeObject.getJsonString("url").getString());
        return JSONB.fromJson(wijzigingObject.toString(), type.getAuditClass());
    }
}
