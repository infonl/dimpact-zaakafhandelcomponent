/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import java.util.List;

public class RestReferenceTable {

    public Long id;

    public String code;

    public String name;

    public boolean isSystemReferenceTable;

    public int valuesCount;

    public List<RestReferenceTableValue> values;

    public RestReferenceTable() {
    }
}
