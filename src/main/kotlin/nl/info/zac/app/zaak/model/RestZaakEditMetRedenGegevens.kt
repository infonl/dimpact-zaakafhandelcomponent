/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaakEditMetRedenGegevens(
    @field:Valid
    var zaak: RestZaakCreateData,

    @field:NotBlank
    var reden: String,

    /**
     * An update that names a groep but no behandelaar keeps the current behandelaar, unless this is set:
     * then it releases the behandelaar and leaves the zaak assigned to the groep only.
     */
    @get:JsonbProperty("isBehandelaarVrijgegeven")
    @set:JsonbProperty("isBehandelaarVrijgegeven")
    var isBehandelaarVrijgegeven: Boolean = false
)
