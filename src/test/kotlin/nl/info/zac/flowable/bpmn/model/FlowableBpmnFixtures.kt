/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import nl.info.zac.admin.model.ZaaktypeBetrokkeneParameters
import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeBrpParameters
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createZaaktypeBpmnConfiguration(
    id: Long? = 1234L,
    zaaktypeUuid: UUID? = UUID.randomUUID(),
    bpmnProcessDefinitionKey: String = "bpmnProcessDefinitionKey",
    zaaktypeOmschrijving: String = "zaaktypeOmschrijving",
    productaanvraagtype: String = "fakeProductaanvraagtype",
    groupName: String = "fakeGroupNaam",
    zaaktypeBetrokkeneParameters: ZaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(),
    zaaktypeBrpParameters: ZaaktypeBrpParameters = createZaaktypeBrpParameters(),
    nietOntvankelijkResultaattype: UUID = UUID.randomUUID(),
    zaaktypeCompletionParameters: List<ZaaktypeCompletionParameters> = emptyList(),
) = ZaaktypeBpmnConfiguration().apply {
    this.id = id
    zaaktypeUuid?.let {
        this.zaaktypeUuid = zaaktypeUuid
    }
    this.bpmnProcessDefinitionKey = bpmnProcessDefinitionKey
    this.zaaktypeOmschrijving = zaaktypeOmschrijving
    this.productaanvraagtype = productaanvraagtype
    this.groepID = groupName
    this.creatiedatum = ZonedDateTime.now()
    this.zaaktypeBetrokkeneParameters = zaaktypeBetrokkeneParameters.also { it.zaaktypeConfiguration = this }
    this.zaaktypeBrpParameters = zaaktypeBrpParameters.also { it.zaaktypeConfiguration = this }
    this.nietOntvankelijkResultaattype = nietOntvankelijkResultaattype
    setZaakbeeindigParameters(zaaktypeCompletionParameters)
}
