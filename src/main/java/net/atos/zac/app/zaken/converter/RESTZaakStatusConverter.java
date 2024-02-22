/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter;

import jakarta.inject.Inject;

import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.ztc.ZTCClientService;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.zac.app.zaken.model.RESTZaakStatus;

public class RESTZaakStatusConverter {

  @Inject private ZRCClientService zrcClientService;

  @Inject private ZTCClientService ztcClientService;

  public RESTZaakStatus convertToRESTZaakStatus(final Status status, final StatusType statustype) {
    final RESTZaakStatus restZaakStatus = new RESTZaakStatus();
    restZaakStatus.toelichting = status.getStatustoelichting();
    restZaakStatus.naam = statustype.getOmschrijving();
    return restZaakStatus;
  }
}
