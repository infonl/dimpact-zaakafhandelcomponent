/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;


import java.util.List;

import net.atos.zac.admin.model.ZaakbeeindigReden;
import net.atos.zac.app.admin.model.RESTZaakbeeindigReden;

public final class RESTZaakbeeindigRedenConverter {

    public static RESTZaakbeeindigReden convertZaakbeeindigReden(final ZaakbeeindigReden zaakbeeindigReden) {
        final RESTZaakbeeindigReden restZaakbeeindigReden = new RESTZaakbeeindigReden();
        restZaakbeeindigReden.id = zaakbeeindigReden.getId().toString();
        restZaakbeeindigReden.naam = zaakbeeindigReden.getNaam();
        return restZaakbeeindigReden;
    }

    public static List<RESTZaakbeeindigReden> convertZaakbeeindigRedenen(final List<ZaakbeeindigReden> zaakbeeindigRedenen) {
        return zaakbeeindigRedenen.stream()
                .map(RESTZaakbeeindigRedenConverter::convertZaakbeeindigReden)
                .toList();
    }

    public static ZaakbeeindigReden convertRESTZaakbeeindigReden(final RESTZaakbeeindigReden restZaakbeeindigReden) {
        final ZaakbeeindigReden zaakbeeindigReden = new ZaakbeeindigReden();
        zaakbeeindigReden.setId(Long.parseLong(restZaakbeeindigReden.id));
        zaakbeeindigReden.setNaam(restZaakbeeindigReden.naam);
        return zaakbeeindigReden;
    }
}
