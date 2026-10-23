/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestHumanTaskParameters(
    @get:JsonbProperty("actief")
    @set:JsonbProperty("actief")
    var isActief: Boolean = false,
    var id: Long? = null,
    var planItemDefinition: RestPlanItemDefinition,
    var doorlooptijd: Int? = null,
    var defaultGroepId: String? = null,
    var formulierDefinitieId: String? = null,
    var referentieTabellen: List<RestHumanTaskReferenceTable>? = emptyList()
)
