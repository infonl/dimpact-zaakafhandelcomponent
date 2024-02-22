/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import java.util.List;

public class RESTCaseDefinition {

  public String key;

  public String naam;

  public List<RESTPlanItemDefinition> humanTaskDefinitions;

  public List<RESTPlanItemDefinition> userEventListenerDefinitions;

  public RESTCaseDefinition() {}

  public RESTCaseDefinition(final String naam, final String key) {
    this.naam = naam;
    this.key = key;
  }
}
