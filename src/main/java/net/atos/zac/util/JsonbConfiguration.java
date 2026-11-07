/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import net.atos.zac.util.time.LocalDateAdapter;
import net.atos.zac.util.time.ZonedDateTimeAdapter;

@Provider
public class JsonbConfiguration implements ContextResolver<Jsonb> {

    private final Jsonb jsonb;

    public JsonbConfiguration() {
        final JsonbConfig jsonbConfig = new JsonbConfig()
                .withAdapters(
                        new ZonedDateTimeAdapter(),
                        new LocalDateAdapter()
                ).withDeserializers(
                        new RESTBAGObjectJsonbDeserializer()
                );
        jsonb = JsonbBuilder.create(jsonbConfig);
    }

    @Override
    public Jsonb getContext(Class<?> type) {
        return jsonb;
    }
}
