/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant.converter;

import java.util.List;
import java.util.stream.Stream;

import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.zac.app.klant.model.klant.RestRoltype;

public class RestRoltypeConverter {

    public static List<RestRoltype> convert(final Stream<RolType> roltypen) {
        return roltypen.map(RestRoltypeConverter::convert).toList();
    }

    public static RestRoltype convert(final RolType roltype) {
        final RestRoltype restRoltype = new RestRoltype();
        restRoltype.uuid = URIUtil.parseUUIDFromResourceURI(roltype.getUrl());
        restRoltype.naam = roltype.getOmschrijving();
        restRoltype.omschrijvingGeneriekEnum = roltype.getOmschrijvingGeneriek();
        return restRoltype;
    }
}
