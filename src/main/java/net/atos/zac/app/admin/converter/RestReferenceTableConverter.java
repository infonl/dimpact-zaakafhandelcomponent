/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.converter;


import net.atos.zac.admin.model.ReferenceTable;
import net.atos.zac.app.admin.model.RestReferenceTable;
import net.atos.zac.app.admin.model.RestReferenceTableValue;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

public final class RestReferenceTableConverter {

    public static RestReferenceTable convert(final ReferenceTable referenceTable, boolean inclusiefWaarden) {
        List<RestReferenceTableValue> values = emptyList();
        if (inclusiefWaarden) {
            values = referenceTable.getValues().stream()
                    .map(RestReferenceValueConverter::convert)
                    .toList();
        }
        return new RestReferenceTable(
                referenceTable.getId(),
                referenceTable.code,
                referenceTable.name,
                referenceTable.isSystemReferenceTable(),
                referenceTable.getValues().size(),
                values
        );
    }

    public static ReferenceTable convert(final RestReferenceTable restReferenceTable) {
        return convert(restReferenceTable, new ReferenceTable());
    }

    public static ReferenceTable convert(final RestReferenceTable restReferenceTable, final ReferenceTable referenceTable) {
        referenceTable.code = restReferenceTable.getCode();
        referenceTable.name = restReferenceTable.getName();
        referenceTable.setValues(
                Objects.requireNonNull(restReferenceTable.getValues())
                        .stream()
                        .map(referenceTableValues -> RestReferenceValueConverter.convert(referenceTable, referenceTableValues))
                        .toList()
            );
        return referenceTable;
    }
}
