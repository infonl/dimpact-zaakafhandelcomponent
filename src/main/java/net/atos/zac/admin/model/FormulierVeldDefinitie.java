/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import nl.info.zac.admin.model.ReferenceTable;

public enum FormulierVeldDefinitie {
    ADVIES(ReferenceTable.Systeem.ADVIES);

    private final ReferenceTable.Systeem defaultTabel;

    FormulierVeldDefinitie(final ReferenceTable.Systeem defaultTabel) {
        this.defaultTabel = defaultTabel;
    }

    public ReferenceTable.Systeem getDefaultTabel() {
        return defaultTabel;
    }
}
