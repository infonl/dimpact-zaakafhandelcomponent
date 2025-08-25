/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import java.util.UUID

fun createZaaktypeBpmnProcessDefinition(
    id: Long = 1234L,
    zaaktypeUuid: UUID = UUID.randomUUID(),
    bpmnProcessDefinitionKey: String = "bpmnProcessDefinitionKey",
    zaaktypeOmschrijving: String = "zaaktypeOmschrijving",
    productaanvraagtype: String = "fakeProductaanvraagtype"
) = ZaaktypeBpmnProcessDefinition().apply {
    this.id = id
    this.zaaktypeUuid = zaaktypeUuid
    this.bpmnProcessDefinitionKey = bpmnProcessDefinitionKey
    this.zaaktypeOmschrijving = zaaktypeOmschrijving
    this.productaanvraagtype = productaanvraagtype
}
