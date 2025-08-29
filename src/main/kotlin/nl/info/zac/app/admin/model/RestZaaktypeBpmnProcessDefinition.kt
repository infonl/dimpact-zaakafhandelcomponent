/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotNull
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaaktypeBpmnProcessDefinition(
    @field:NotNull
    var zaaktypeUuid: UUID,

    @field:NotNull
    var zaaktypeOmschrijving: String,

    var productaanvraagtype: String?,

    var groepNaam: String?
)
