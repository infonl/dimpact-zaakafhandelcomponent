/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;


import net.atos.zac.admin.model.ReferenceTable;
import net.atos.zac.app.admin.model.RestReferenceTable;

public final class RestReferenceTableConverter {

    public static RestReferenceTable convert(final ReferenceTable referenceTable, boolean inclusiefWaarden) {
        final RestReferenceTable restReferenceTable = new RestReferenceTable();
        restReferenceTable.id = referenceTable.getId();
        restReferenceTable.code = referenceTable.code;
        restReferenceTable.naam = referenceTable.naam;
        restReferenceTable.systeem = referenceTable.isSysteem();
        restReferenceTable.aantalWaarden = referenceTable.getWaarden().size();
        if (inclusiefWaarden) {
            restReferenceTable.waarden = referenceTable.getWaarden().stream()
                    .map(RestReferenceValueConverter::convert)
                    .toList();
        }
        return restReferenceTable;
    }

    public static ReferenceTable convert(final RestReferenceTable restReferenceTable) {
        return convert(restReferenceTable, new ReferenceTable());
    }

    public static ReferenceTable convert(final RestReferenceTable restReferenceTable, final ReferenceTable referenceTable) {
        referenceTable.code = restReferenceTable.code;
        referenceTable.naam = restReferenceTable.naam;
        referenceTable.setWaarden(restReferenceTable.waarden.stream()
                .map(RestReferenceValueConverter::convert)
                .toList());
        return referenceTable;
    }
}
