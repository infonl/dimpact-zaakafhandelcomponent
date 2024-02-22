/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.client.bag.model.StatusVerblijfsobject;

public class StatusVerblijfsobjectEnumAdapter
    implements JsonbAdapter<StatusVerblijfsobject, String> {

  @Override
  public String adaptToJson(final StatusVerblijfsobject statusVerblijfsobject) {
    return statusVerblijfsobject.toString();
  }

  @Override
  public StatusVerblijfsobject adaptFromJson(final String json) {
    return StatusVerblijfsobject.fromValue(json);
  }
}
