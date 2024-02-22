/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter;

import static net.atos.client.zgw.ztc.util.ZaakTypeUtil.isNuGeldig;
import static net.atos.client.zgw.ztc.util.ZaakTypeUtil.isServicenormBeschikbaar;

import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.app.admin.model.RESTZaaktypeOverzicht;

public class RESTZaaktypeOverzichtConverter {

  public RESTZaaktypeOverzicht convert(final ZaakType zaaktype) {
    final RESTZaaktypeOverzicht restZaaktype = new RESTZaaktypeOverzicht();
    restZaaktype.uuid = URIUtil.parseUUIDFromResourceURI(zaaktype.getUrl());
    restZaaktype.identificatie = zaaktype.getIdentificatie();
    restZaaktype.doel = zaaktype.getDoel();
    restZaaktype.omschrijving = zaaktype.getOmschrijving();
    restZaaktype.servicenorm = isServicenormBeschikbaar(zaaktype);
    restZaaktype.versiedatum = zaaktype.getVersiedatum();
    restZaaktype.nuGeldig = isNuGeldig(zaaktype);
    restZaaktype.beginGeldigheid = zaaktype.getBeginGeldigheid();
    restZaaktype.eindeGeldigheid = zaaktype.getEindeGeldigheid();
    restZaaktype.vertrouwelijkheidaanduiding = zaaktype.getVertrouwelijkheidaanduiding();
    return restZaaktype;
  }
}
