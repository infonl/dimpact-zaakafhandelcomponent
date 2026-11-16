/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * The process definition that a zaaktype configuration is bound to, as the Zaakafhandelparameters page shows it.
 */
@NoArgConstructor
@AllOpen
data class RestProcessDefinitionOverzicht(
    var key: String,
    var naam: String? = null
)

fun RestProcessDefinition.toRestProcessDefinitionOverzicht() = RestProcessDefinitionOverzicht(key = key, naam = naam)
