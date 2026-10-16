/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import net.atos.zac.app.admin.model.RESTPlanItemDefinition
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * The process definition that the zaken of a zaaktype run with, together with the plan items that ZAC lets a
 * beheerder configure for it.
 */
@NoArgConstructor
@AllOpen
data class RestProcessDefinition(
    @field:NotNull
    var processEngine: ProcessEngine,
    @field:NotBlank
    var key: String,
    var naam: String? = null,
    var humanTaskDefinitions: List<RESTPlanItemDefinition> = emptyList(),
    var userEventListenerDefinitions: List<RESTPlanItemDefinition> = emptyList()
)
