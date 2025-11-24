/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import nl.info.zac.admin.model.ZaaktypeBpmnConfiguration
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
}
