/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.notities.model

import net.atos.zac.notities.model.Notitie
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestNotitie(
    var id: Long?,
    var zaakUUID: UUID,
    var tekst: String,
    var tijdstipLaatsteWijziging: ZonedDateTime,
    var gebruikersnaamMedewerker: String,
    var voornaamAchternaamMedewerker: String?,
    var bewerkenToegestaan: Boolean
)

fun RestNotitie.toNotitie() = Notitie().apply {
    id = this@toNotitie.id ?: 0
    zaakUUID = this@toNotitie.zaakUUID
    tekst = this@toNotitie.tekst
    tijdstipLaatsteWijziging = this@toNotitie.tijdstipLaatsteWijziging
    gebruikersnaamMedewerker = this@toNotitie.gebruikersnaamMedewerker
}
