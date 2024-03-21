/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.util.*

class RESTZaakKoppelGegevens {
    var zaakUuid: UUID? = null

    var teKoppelenZaakUuid: UUID? = null

    var relatieType: RelatieType? = null

    var reverseRelatieType: RelatieType? = null
}
