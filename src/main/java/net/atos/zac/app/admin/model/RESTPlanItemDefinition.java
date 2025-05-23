/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import net.atos.zac.admin.model.FormulierDefinitie;
import nl.info.zac.app.planitems.converter.FormulierKoppelingConverterKt;
import nl.info.zac.app.planitems.model.PlanItemType;

public class RESTPlanItemDefinition {

    public String id;

    public String naam;

    public PlanItemType type;

    public FormulierDefinitie defaultFormulierDefinitie;

    public RESTPlanItemDefinition() {
    }

    public RESTPlanItemDefinition(final String id, final String naam, final PlanItemType type) {
        this.id = id;
        this.naam = naam;
        this.type = type;
        this.defaultFormulierDefinitie = FormulierKoppelingConverterKt.toFormulierDefinitie(id);
    }
}
