/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model;

import nl.info.zac.admin.model.ReferenceTable;

public enum FormulierVeldDefinitie {
    ADVIES(ReferenceTable.SystemReferenceTable.ADVIES);

    private final ReferenceTable.SystemReferenceTable defaultTabel;

    FormulierVeldDefinitie(final ReferenceTable.SystemReferenceTable defaultTabel) {
        this.defaultTabel = defaultTabel;
    }

    public ReferenceTable.SystemReferenceTable getDefaultTabel() {
        return defaultTabel;
    }
}
