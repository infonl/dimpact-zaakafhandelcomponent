/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaakLinkData(
    var zaakUuid: UUID,

    var teKoppelenZaakUuid: UUID,

    var relatieType: RelatieType,

    var reverseRelatieType: RelatieType?
)
