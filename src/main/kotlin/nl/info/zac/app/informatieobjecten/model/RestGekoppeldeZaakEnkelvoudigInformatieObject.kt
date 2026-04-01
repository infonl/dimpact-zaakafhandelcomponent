/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.informatieobjecten.model

import nl.info.zac.app.zaak.model.RelatieType
import java.util.UUID

class RestGekoppeldeZaakEnkelvoudigInformatieObject : RestEnkelvoudigInformatieobject() {
    var relatieType: RelatieType? = null

    var zaakIdentificatie: String? = null

    var zaakUUID: UUID? = null
}
