/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;


import net.atos.zac.app.admin.model.RESTZaakbeeindigReden;
import net.atos.zac.zaaksturing.model.ZaakbeeindigReden;

public final class RESTZaakbeeindigRedenConverter {

    public static RESTZaakbeeindigReden convertZaakbeeindigReden(final ZaakbeeindigReden zaakbeeindigReden) {
        final RESTZaakbeeindigReden restZaakbeeindigReden = new RESTZaakbeeindigReden();
        restZaakbeeindigReden.id = zaakbeeindigReden.getId();
        restZaakbeeindigReden.naam = zaakbeeindigReden.getNaam();
        return restZaakbeeindigReden;
    }

    public static ZaakbeeindigReden convertRESTZaakbeeindigReden(final RESTZaakbeeindigReden restZaakbeeindigReden) {
        final ZaakbeeindigReden zaakbeeindigReden = new ZaakbeeindigReden();
        zaakbeeindigReden.setId(restZaakbeeindigReden.id);
        zaakbeeindigReden.setNaam(restZaakbeeindigReden.naam);
        return zaakbeeindigReden;
    }
}
