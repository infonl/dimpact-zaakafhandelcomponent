/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.note.model.Note
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestNote(
    var id: Long?,
    var zaakUUID: UUID,

    @field:JsonbProperty("tekst")
    var text: String,

    /**
     * Date-time when the note was last modified.
     */
    @field:JsonbProperty("tijdstipLaatsteWijziging")
    var dateTimeLastModified: ZonedDateTime?,

    @field:JsonbProperty("gebruikersnaamMedewerker")
    var employeeUsername: String,

    @field:JsonbProperty("voornaamAchternaamMedewerker")
    var employeeFullname: String?,

    @field:JsonbProperty("bewerkenToegestaan")
    var updatingAllowed: Boolean
)

fun RestNote.toNote() = Note().apply {
    id = this@toNote.id ?: 0
    zaakUUID = this@toNote.zaakUUID
    text = this@toNote.text
    dateTimeLastModified = ZonedDateTime.now()
    employeeUsername = this@toNote.employeeUsername
}
