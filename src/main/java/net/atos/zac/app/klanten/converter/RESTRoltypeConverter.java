/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klanten.converter;

import java.util.List;
import java.util.stream.Stream;

import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.zac.app.klanten.model.klant.RESTRoltype;

public class RESTRoltypeConverter {

    public List<RESTRoltype> convert(final Stream<RolType> roltypen) {
        return roltypen.map(this::convert).toList();
    }

    public RESTRoltype convert(final RolType roltype) {
        final RESTRoltype restRoltype = new RESTRoltype();
        restRoltype.uuid = URIUtil.parseUUIDFromResourceURI(roltype.getUrl());
        restRoltype.naam = roltype.getOmschrijving();
        restRoltype.omschrijvingGeneriekEnum = roltype.getOmschrijvingGeneriek();
        return restRoltype;
    }
}
