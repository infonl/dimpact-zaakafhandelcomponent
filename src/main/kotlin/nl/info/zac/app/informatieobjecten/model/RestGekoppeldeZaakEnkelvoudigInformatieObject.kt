/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.model.RelatieType
import java.util.UUID

// Cannot be a data class because Kotlin does not allow data classes to extend other data classes
@NoArgConstructor
@AllOpen
class RestGekoppeldeZaakEnkelvoudigInformatieObject : RestEnkelvoudigInformatieobject() {
    var relatieType: RelatieType? = null

    var zaakIdentificatie: String? = null

    @get:JsonbProperty("isZaakspecifiekGeautoriseerd")
    var isZaakspecifiekGeautoriseerd: Boolean = false

    var zaakUUID: UUID? = null
}
