/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;

import static nl.info.client.zgw.util.ZgwUriUtilsKt.extractUuid;
import static nl.info.client.zgw.ztc.model.extensions.ZaakTypeExtensionsKt.isNuGeldig;
import static nl.info.client.zgw.ztc.model.extensions.ZaakTypeExtensionsKt.isServicenormBeschikbaar;

import net.atos.zac.app.admin.model.RESTZaaktypeOverzicht;
import nl.info.client.zgw.ztc.model.generated.ZaakType;

public final class RESTZaaktypeOverzichtConverter {

    public static RESTZaaktypeOverzicht convert(final ZaakType zaaktype) {
        final RESTZaaktypeOverzicht restZaaktype = new RESTZaaktypeOverzicht();
        restZaaktype.uuid = extractUuid(zaaktype.getUrl());
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
