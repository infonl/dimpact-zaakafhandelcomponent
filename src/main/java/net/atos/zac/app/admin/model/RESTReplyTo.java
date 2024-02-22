/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model;

import net.atos.zac.zaaksturing.model.ZaakAfzender;

public class RESTReplyTo {

  public String mail;

  public boolean speciaal;

  public RESTReplyTo() {}

  public RESTReplyTo(ZaakAfzender.Speciaal speciaal) {
    this.mail = speciaal.name();
    this.speciaal = true;
  }
}
