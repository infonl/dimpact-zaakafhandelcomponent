/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;

public class JsonbConfiguration implements ContextResolver<Jsonb> {

  private Jsonb jsonb;

  public JsonbConfiguration() {
    final JsonbConfig jsonbConfig =
        new JsonbConfig()
            .withAdapters(
                new IndicatieEnumAdapter(),
                new StatusNaamgevingEnumAdapter(),
                new StatusPandEnumAdapter(),
                new StatusWoonplaatsEnumAdapter(),
                new StatusVerblijfsobjectEnumAdapter(),
                new TypeAdresseerbaarObjectEnumAdapter(),
                new GebruiksdoelEnumAdapter(),
                new TypeOpenbareRuimteEnumAdapter());
    jsonb = JsonbBuilder.create(jsonbConfig);
  }

  @Override
  public Jsonb getContext(Class<?> type) {
    return jsonb;
  }
}
