/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.Valid;

import net.atos.zac.app.identity.model.RESTGroup;
import net.atos.zac.app.identity.model.RESTUser;

public class RESTHumanTaskData {

  public String planItemInstanceId;

  public RESTGroup groep;

  @Valid public RESTUser medewerker;

  public LocalDate fataledatum;

  public String toelichting;

  public Map<String, String> taakdata;

  public RESTTaakStuurGegevens taakStuurGegevens;
}
