/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.formio

import nl.info.zac.flowable.bpmn.model.BpmnProcessDefinitionTaskForm

@Suppress("LongParameterList")
fun createBpmnProcessDefinitionTaskForm(
    id: Long = 124L,
    bpmnProcessDefinition: String = "fakeBpmnProcessDefinition",
    bpmnProcessDefinitionVersion: Int = 1,
    name: String = "testForm",
    title: String = "fakeTitle",
    content: String = """{ "fakeKey": "fakeValue" }"""
) = BpmnProcessDefinitionTaskForm().apply {
    this.id = id
    this.bpmnProcessDefinitionKey = bpmnProcessDefinition
    this.bpmnProcessDefinitionVersion = bpmnProcessDefinitionVersion
    this.name = name
    this.title = title
    this.content = content
}
