/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;

public class JsonbConfiguration implements ContextResolver<Jsonb> {

  private Jsonb jsonb;

  public JsonbConfiguration() {
    final JsonbConfig jsonbConfig =
        new JsonbConfig().withDeserializers(new PersonenQueryResponseJsonbDeserializer());
    jsonb = JsonbBuilder.create(jsonbConfig);
  }

  @Override
  public Jsonb getContext(Class<?> type) {
    return jsonb;
  }
}
