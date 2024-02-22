/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.converter;

import net.atos.client.zgw.drc.model.generated.Ondertekening;
import net.atos.zac.app.informatieobjecten.model.RESTOndertekening;

public class RESTOndertekeningConverter {

  public RESTOndertekening convert(final Ondertekening ondertekening) {
    final RESTOndertekening restOndertekening = new RESTOndertekening();
    restOndertekening.soort = ondertekening.getSoort().value();
    restOndertekening.datum = ondertekening.getDatum();
    return restOndertekening;
  }
}
