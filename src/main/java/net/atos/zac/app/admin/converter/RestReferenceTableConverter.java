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
        restReferenceTable.naam = referenceTable.name;
        restReferenceTable.systeem = referenceTable.isSystemReferenceTable();
        restReferenceTable.aantalWaarden = referenceTable.getValues().size();
        if (inclusiefWaarden) {
            restReferenceTable.waarden = referenceTable.getValues().stream()
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
        referenceTable.name = restReferenceTable.naam;
        referenceTable.setValues(
                restReferenceTable.waarden
                        .stream()
                        .map(referenceTableValues -> RestReferenceValueConverter.convert(referenceTable, referenceTableValues))
                        .toList()
        );
        return referenceTable;
    }
}
