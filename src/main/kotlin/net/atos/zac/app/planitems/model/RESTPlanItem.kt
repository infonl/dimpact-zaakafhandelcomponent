/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import net.atos.zac.admin.model.FormulierDefinitie
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
data class RESTPlanItem(
    var id: String,

    var naam: String,

    var type: PlanItemType,

    var tabellen: MutableMap<String?, List<String>> = mutableMapOf(),

    var zaakUuid: UUID,

    var groepId: String? = null,

    var actief: Boolean = false,

    var formulierDefinitie: FormulierDefinitie? = null,

    var userEventListenerActie: UserEventListenerActie? = null,

    var toelichting: String? = null,

    var fataleDatum: LocalDate? = null,
)
