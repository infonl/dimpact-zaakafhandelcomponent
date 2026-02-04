/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;


import java.util.List;

import net.atos.zac.app.admin.model.RestZaakbeeindigReden;
import nl.info.zac.admin.model.ZaakbeeindigReden;

public final class RESTZaakbeeindigRedenConverter {

    public static RestZaakbeeindigReden convertZaakbeeindigReden(final ZaakbeeindigReden zaakbeeindigReden) {
        final RestZaakbeeindigReden restZaakbeeindigReden = new RestZaakbeeindigReden();
        restZaakbeeindigReden.id = zaakbeeindigReden.getId().toString();
        restZaakbeeindigReden.naam = zaakbeeindigReden.getNaam();
        return restZaakbeeindigReden;
    }

    public static List<RestZaakbeeindigReden> convertZaakbeeindigRedenen(final List<ZaakbeeindigReden> zaakbeeindigRedenen) {
        return zaakbeeindigRedenen.stream()
                .map(RESTZaakbeeindigRedenConverter::convertZaakbeeindigReden)
                .toList();
    }

    public static ZaakbeeindigReden convertRESTZaakbeeindigReden(final RestZaakbeeindigReden restZaakbeeindigReden) {
        final ZaakbeeindigReden zaakbeeindigReden = new ZaakbeeindigReden();
        zaakbeeindigReden.setId(Long.parseLong(restZaakbeeindigReden.id));
        zaakbeeindigReden.setNaam(restZaakbeeindigReden.naam);
        return zaakbeeindigReden;
    }
}
