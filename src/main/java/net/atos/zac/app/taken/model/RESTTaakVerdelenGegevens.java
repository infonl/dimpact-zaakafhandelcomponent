/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model;

import java.util.List;

public class RESTTaakVerdelenGegevens {

  public List<RESTTaakVerdelenTaak> taken;

  public String behandelaarGebruikersnaam;

  public String groepId;

  public String reden;
}
