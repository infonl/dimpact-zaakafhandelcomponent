/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
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
    var zaakbeeindigParameters: List<RestZaakbeeindigParameter> = emptyList(),

    var smartDocuments: RestSmartDocuments? = null
)

fun RestZaaktypeBpmnConfiguration.toZaaktypeConfiguration() = ZaaktypeConfiguration().apply {
    id = this@toZaaktypeConfiguration.id
    zaaktypeUuid = this@toZaaktypeConfiguration.zaaktypeUuid
    bindTo(ProcessEngine.BPMN, this@toZaaktypeConfiguration.bpmnProcessDefinitionKey)
    zaaktypeOmschrijving = this@toZaaktypeConfiguration.zaaktypeOmschrijving
    productaanvraagtype = this@toZaaktypeConfiguration.productaanvraagtype
    defaultBehandelaarId = this@toZaaktypeConfiguration.defaultBehandelaarId
    groepID = this@toZaaktypeConfiguration.groepNaam
    creatiedatum = this@toZaaktypeConfiguration.creatiedatum ?: ZonedDateTime.now()
    isSmartDocumentsEnabled = this@toZaaktypeConfiguration.smartDocuments?.isEnabledForZaaktype ?: false
    zaaktypeBetrokkeneParameters =
        this@toZaaktypeConfiguration.betrokkeneKoppelingen?.toZaaktypeBetrokkenParameters(this)
    zaaktypeBrpParameters =
        this@toZaaktypeConfiguration.brpDoelbindingen?.toZaaktypeBrpParameters(this)
}
