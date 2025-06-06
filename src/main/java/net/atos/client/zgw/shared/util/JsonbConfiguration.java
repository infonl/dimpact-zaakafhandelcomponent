/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;

import nl.info.client.zgw.zrc.jsonb.DeleteGeoJSONGeometryJsonbSerializer;
import nl.info.client.zgw.zrc.jsonb.RolJsonbDeserializer;
import nl.info.client.zgw.zrc.jsonb.ZaakObjectJsonbDeserializer;

public class JsonbConfiguration implements ContextResolver<Jsonb> {

    private final Jsonb jsonb;

    public JsonbConfiguration() {
        final JsonbConfig jsonbConfig = new JsonbConfig()
                .withDeserializers(
                        new RolJsonbDeserializer(),
                        new ZaakObjectJsonbDeserializer(),
                        new URIJsonbDeserializer()
                )
                .withSerializers(
                        new DeleteGeoJSONGeometryJsonbSerializer()
                );
        jsonb = JsonbBuilder.create(jsonbConfig);
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}
