/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.flowable.engine.repository.ProcessDefinition

@NoArgConstructor
@AllOpen
data class RestZaakBpmnProcessDefinition(
    var processDefinitionKey: String,
    var processDefinitionName: String,
    var processDefinitionVersion: Int
)

fun ProcessDefinition.toRestZaakBpmnProcessDefinition() = RestZaakBpmnProcessDefinition(
    processDefinitionKey = this.key,
    processDefinitionName = this.name,
    processDefinitionVersion = this.version
)
