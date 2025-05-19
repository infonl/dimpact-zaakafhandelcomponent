/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.notities.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.notities.model.Notitie
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestNotitie(
    var id: Long?,
    var zaakUUID: UUID,

    @get:JsonbProperty("tekst")
    var text: String,

    /**
     * Date-time when the note was last modified.
     */
    @get:JsonbProperty("tijdstipLaatsteWijziging")
    var dateTimeLastModified: ZonedDateTime?,

    @get:JsonbProperty("gebruikersnaamMedewerker")
    var employeeUsername: String,

    @get:JsonbProperty("voornaamAchternaamMedewerker")
    var employeeFullname: String?,

    @get:JsonbProperty("bewerkenToegestaan")
    var updatingAllowed: Boolean
)

fun RestNotitie.toNotitie() = Notitie().apply {
    id = this@toNotitie.id ?: 0
    zaakUUID = this@toNotitie.zaakUUID
    this.text = this@toNotitie.text
    this.dateTimeLastModified = ZonedDateTime.now()
    this.employeeUsername = this@toNotitie.employeeUsername
}
