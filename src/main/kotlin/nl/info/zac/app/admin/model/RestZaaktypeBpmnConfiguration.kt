/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotNull
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaaktypeBpmnConfiguration(
    @field:NotNull
    var zaaktype: RestZaaktypeOverzicht,

    var bpmnProcessDefinitionKey: String,

    var productaanvraagtype: String?,

    @field:NotNull
    var groepNaam: String
)
