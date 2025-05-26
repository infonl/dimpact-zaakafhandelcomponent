/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.zac.app.note.model.RestNote
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.note.model.Note

class NoteConverter @Inject constructor(
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    fun toRestNote(note: Note): RestNote {
        val medewerker = identityService.readUser(note.employeeUsername)
        return RestNote(
            id = note.id,
            zaakUUID = note.zaakUUID,
            text = note.text,
            dateTimeLastModified = note.dateTimeLastModified,
            employeeUsername = note.employeeUsername,
            employeeFullname = "${medewerker.firstName} ${medewerker.lastName}",
            // updating a note is only allowed if the logged-in user is the same as the employee who created the note
            updatingAllowed = loggedInUserInstance.get().id == note.employeeUsername
        )
    }
}
