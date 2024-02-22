/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter;

import java.net.URI;

import jakarta.inject.Inject;

import net.atos.client.zgw.zrc.ZRCClientService;
import net.atos.client.zgw.zrc.model.generated.Resultaat;
import net.atos.zac.app.zaken.model.RESTZaakResultaat;

public class RESTZaakResultaatConverter {

  @Inject private ZRCClientService zrcClientService;

  @Inject private RESTResultaattypeConverter restResultaattypeConverter;

  public RESTZaakResultaat convert(final URI resultaatURI) {
    if (resultaatURI != null) {
      final Resultaat resultaat = zrcClientService.readResultaat(resultaatURI);
      final RESTZaakResultaat restZaakResultaat = new RESTZaakResultaat();
      restZaakResultaat.toelichting = resultaat.getToelichting();
      restZaakResultaat.resultaattype =
          restResultaattypeConverter.convertResultaattypeUri(resultaat.getResultaattype());
      return restZaakResultaat;
    }
    return null;
  }
}
