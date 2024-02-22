/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class SmartDocument {

  @JsonbProperty("Selection")
  public Selection selection;
}
