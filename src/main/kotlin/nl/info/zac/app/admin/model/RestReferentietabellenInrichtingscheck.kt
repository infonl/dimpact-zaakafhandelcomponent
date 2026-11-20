/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestReferentietabellenInrichtingscheck(
    @get:JsonbProperty("isCommunicatiekanaalEformulierAanwezig")
    @set:JsonbProperty("isCommunicatiekanaalEformulierAanwezig")
    var isCommunicatiekanaalEformulierAanwezig: Boolean
)
