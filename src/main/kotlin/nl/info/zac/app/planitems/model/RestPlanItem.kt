/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.admin.model.FormulierDefinitie
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
data class RestPlanItem(
    var id: String,

    var naam: String,

    var type: PlanItemType,

    var tabellen: MutableMap<String?, List<String>> = mutableMapOf(),

    var zaakUuid: UUID,

    var groepId: String? = null,

    @get:JsonbProperty("isActief")
    @set:JsonbProperty("isActief")
    var isActief: Boolean = false,

    var formulierDefinitie: FormulierDefinitie? = null,

    var userEventListenerActie: UserEventListenerActie? = null,

    var toelichting: String? = null,

    var fataleDatum: LocalDate? = null,
)
