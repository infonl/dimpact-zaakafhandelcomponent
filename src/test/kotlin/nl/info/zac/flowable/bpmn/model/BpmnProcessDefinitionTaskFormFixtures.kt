/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.bpmn.model

@Suppress("LongParameterList")
fun createBpmnProcessDefinitionTaskForm(
    id: Long = 124L,
    bpmnProcessDefinitionKey: String = "fakeBpmnProcessDefinition",
    bpmnProcessDefinitionVersion: Int = 1,
    name: String = "testForm",
    title: String = "fakeTitle",
    content: String = """{ "fakeKey": "fakeValue" }, "title": "$title"}"""
) = BpmnProcessDefinitionTaskForm().apply {
    this.id = id
    this.bpmnProcessDefinitionKey = bpmnProcessDefinitionKey
    this.bpmnProcessDefinitionVersion = bpmnProcessDefinitionVersion
    this.name = name
    this.title = title
    this.content = content
}
