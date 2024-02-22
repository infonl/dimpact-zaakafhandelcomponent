/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;

import net.atos.client.zgw.zrc.util.GeometryJsonbDeserializer;
import net.atos.client.zgw.zrc.util.RolJsonbDeserializer;
import net.atos.client.zgw.zrc.util.ZaakObjectJsonbDeserializer;

public class JsonbConfiguration implements ContextResolver<Jsonb> {

    private Jsonb jsonb;

    public JsonbConfiguration() {
        final JsonbConfig jsonbConfig = new JsonbConfig().withDeserializers(
                new RolJsonbDeserializer(), new ZaakObjectJsonbDeserializer(), new GeometryJsonbDeserializer(), new URIJsonbDeserializer());
        jsonb = JsonbBuilder.create(jsonbConfig);
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}
