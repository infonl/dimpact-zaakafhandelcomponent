/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren.model

import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardId
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardInstelling
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestDashboardCardInstelling(
    var id: Long? = null,
    var cardId: DashboardCardId,
    var signaleringType: SignaleringType.Type? = null,
    var column: Int = 0,
    var row: Int = 0
)

fun DashboardCardInstelling.toRestDashboardCardInstelling() = RestDashboardCardInstelling(
    id = id,
    cardId = cardId,
    column = kolom,
    row = volgorde
)

fun RestDashboardCardInstelling.toDashboardCardInstelling() =
    DashboardCardInstelling().apply {
        id = this@toDashboardCardInstelling.id
        cardId = this@toDashboardCardInstelling.cardId
        signaleringType = this@toDashboardCardInstelling.signaleringType
        kolom = this@toDashboardCardInstelling.column
        volgorde = this@toDashboardCardInstelling.row
    }
