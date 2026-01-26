/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import net.atos.zac.app.admin.model.RESTZaakbeeindigParameter
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaaktypeBpmnConfiguration(
    var id: Long? = null,

    @field:NotNull
    var zaaktypeUuid: UUID,

    @field:NotBlank
    var zaaktypeOmschrijving: String,

    var bpmnProcessDefinitionKey: String,

    var productaanvraagtype: String?,

    @field:NotBlank
    var groepNaam: String? = null,

    var creatiedatum: ZonedDateTime?,

    // The frontend currently requires this field to be non-null
    var betrokkeneKoppelingen: RestBetrokkeneKoppelingen = RestBetrokkeneKoppelingen(),

    // The frontend currently requires this field to be non-null
    var brpDoelbindingen: RestBrpDoelbindingen = RestBrpDoelbindingen(),

    var nietOntvankelijkResultaattype: UUID? = null,

    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakbeeindigParameters: List<RESTZaakbeeindigParameter> = emptyList(),
)
