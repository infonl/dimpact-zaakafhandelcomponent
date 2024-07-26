/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import net.atos.zac.admin.model.FormulierVeldDefinitie;

public class RestHumanTaskReferenceTable {

    public Long id;

    public String veld;

    public RestReferenceTable tabel;

    public RestHumanTaskReferenceTable() {
    }

    public RestHumanTaskReferenceTable(final FormulierVeldDefinitie veldDefinitie) {
        veld = veldDefinitie.name();
    }
}
