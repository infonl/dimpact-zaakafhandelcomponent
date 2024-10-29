/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import net.atos.zac.admin.model.FormulierDefinitie
import java.time.LocalDate
import java.util.UUID

class RESTPlanItem {
    var id: String? = null

    var naam: String? = null

    var type: PlanItemType? = null

    var groepId: String? = null

    var actief: Boolean = false

    var formulierDefinitie: FormulierDefinitie? = null

    var tabellen: MutableMap<String?, List<String>> = HashMap()

    var zaakUuid: UUID? = null

    var userEventListenerActie: UserEventListenerActie? = null

    var toelichting: String? = null

    var fataleDatum: LocalDate? = null
}
