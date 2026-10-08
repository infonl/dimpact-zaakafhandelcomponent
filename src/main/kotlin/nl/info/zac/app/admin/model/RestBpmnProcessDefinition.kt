/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@AllOpen
@NoArgConstructor
data class RestBpmnProcessDefinition(
    var id: String,
    var name: String,
    var version: Int,
    var key: String,
    var details: RestBpmnProcessDefinitionDetails? = null,
)

@AllOpen
@NoArgConstructor
data class RestBpmnProcessDefinitionDetails(
    @get:JsonbProperty("isInUse")
    @set:JsonbProperty("isInUse")
    var isInUse: Boolean = false,

    var documentation: String? = null,
    var modificationDate: ZonedDateTime? = null,
    var uploadDate: ZonedDateTime? = null,
    var forms: List<RestBpmnProcessDefinitionForm> = emptyList(),
    var orphanedForms: List<RestBpmnProcessDefinitionForm> = emptyList(),
)

@AllOpen
@NoArgConstructor
data class RestBpmnProcessDefinitionForm(
    var formKey: String,
    var title: String? = null,

    @get:JsonbProperty("isUploaded")
    @set:JsonbProperty("isUploaded")
    var isUploaded: Boolean = false,
)
