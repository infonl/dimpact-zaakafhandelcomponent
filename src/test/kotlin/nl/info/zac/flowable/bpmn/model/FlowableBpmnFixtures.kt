/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

import java.util.UUID

fun createZaaktypeBpmnProcessDefinition(
    id: Long = 1234L,
    zaaktypeUuid: UUID = UUID.randomUUID(),
    bpmnProcessDefinitionKey: String = "bpmnProcessDefinitionKey"
) = ZaaktypeBpmnProcessDefinition().apply {
    this.id = id
    this.zaaktypeUuid = zaaktypeUuid
    this.bpmnProcessDefinitionKey = bpmnProcessDefinitionKey
}
