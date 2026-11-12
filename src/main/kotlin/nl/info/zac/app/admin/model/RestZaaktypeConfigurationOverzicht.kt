/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

/**
 * A row of the Zaakafhandelparameters page: the configuration of a zaaktype version, with only the fields that the
 * page shows, filters and sorts on. A zaaktype version without a saved configuration has no [processDefinition] and
 * no [creatiedatum].
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeConfigurationOverzicht(
    var zaaktype: RestZaaktypeBeheerOverzicht,
    var processDefinition: RestProcessDefinitionOverzicht? = null,
    var creatiedatum: ZonedDateTime? = null,

    @get:JsonbProperty("isValide")
    @set:JsonbProperty("isValide")
    var isValide: Boolean = false
)
