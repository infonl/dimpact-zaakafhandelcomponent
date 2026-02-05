/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.app.zaak.model.RestResultaattype
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

    var creatiedatum: ZonedDateTime? = null,

    var defaultBehandelaarId: String? = null,

    var betrokkeneKoppelingen: RestBetrokkeneKoppelingen? = null,

    var brpDoelbindingen: RestBrpDoelbindingen? = null,

    var zaakNietOntvankelijkResultaattype: RestResultaattype? = null,

    /**
     * The frontend currently requires this field to be non-null
     */
    var zaakbeeindigParameters: List<RestZaakbeeindigParameter> = emptyList()
)

fun RestZaaktypeBpmnConfiguration.toZaaktypeBpmnConfiguration() = ZaaktypeBpmnConfiguration().apply {
    id = this@toZaaktypeBpmnConfiguration.id
    zaaktypeUuid = this@toZaaktypeBpmnConfiguration.zaaktypeUuid
    bpmnProcessDefinitionKey = this@toZaaktypeBpmnConfiguration.bpmnProcessDefinitionKey
    zaaktypeOmschrijving = this@toZaaktypeBpmnConfiguration.zaaktypeOmschrijving
    productaanvraagtype = this@toZaaktypeBpmnConfiguration.productaanvraagtype
    defaultBehandelaarId = this@toZaaktypeBpmnConfiguration.defaultBehandelaarId
    groepID = this@toZaaktypeBpmnConfiguration.groepNaam
    creatiedatum = this@toZaaktypeBpmnConfiguration.creatiedatum ?: ZonedDateTime.now()
    zaaktypeBetrokkeneParameters =
        this@toZaaktypeBpmnConfiguration.betrokkeneKoppelingen?.toZaaktypeBetrokkenParameters(this)
    zaaktypeBrpParameters =
        this@toZaaktypeBpmnConfiguration.brpDoelbindingen?.toZaaktypeBrpParameters(this)
    nietOntvankelijkResultaattype = this@toZaaktypeBpmnConfiguration.zaakNietOntvankelijkResultaattype?.id
    setZaakbeeindigParameters(
        this@toZaaktypeBpmnConfiguration.zaakbeeindigParameters.toZaaktypeCompletionParametersList()
    )
}
