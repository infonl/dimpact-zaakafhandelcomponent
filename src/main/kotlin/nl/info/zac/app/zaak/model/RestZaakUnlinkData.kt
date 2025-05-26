/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestZaakUnlinkData(
    var zaakUuid: UUID,

    var gekoppeldeZaakIdentificatie: String,

    var relatieType: RelatieType,

    var reden: String
)
