/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.model.RelatieType
import java.util.UUID

/**
 * The existing zaak that a new zaak is linked to when it is created.
 * [relatieType] is the role of the new zaak: [RelatieType.DEELZAAK] makes the new zaak a deelzaak of [zaakUuid].
 */
@NoArgConstructor
@AllOpen
data class RestZaakAanmaakKoppeling(
    var zaakUuid: UUID,

    var relatieType: RelatieType
)
