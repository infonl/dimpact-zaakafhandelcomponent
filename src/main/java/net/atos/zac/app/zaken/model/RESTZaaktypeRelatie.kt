/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.util.*

data class RESTZaaktypeRelatie(
    val zaaktypeUuid: UUID,

    val relatieType: RelatieType
)
