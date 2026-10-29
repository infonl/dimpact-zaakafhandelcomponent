/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.admin.model.toFormulierDefinitie
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestPlanItemDefinition(
    var id: String,
    var naam: String?,
    var type: PlanItemType
) {
    val defaultFormulierDefinitie: FormulierDefinitie
        get() = id.toFormulierDefinitie()
}
