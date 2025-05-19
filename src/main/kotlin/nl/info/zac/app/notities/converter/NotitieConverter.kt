/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.notities.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.zac.app.notities.model.RestNotitie
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.notities.model.Notitie

class NotitieConverter @Inject constructor(
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    fun toRestNotitie(notitie: Notitie): RestNotitie {
        val medewerker = identityService.readUser(notitie.employeeUsername)
        return RestNotitie(
            id = notitie.id,
            zaakUUID = notitie.zaakUUID,
            text = notitie.text,
            dateTimeLastModified = notitie.dateTimeLastModified,
            employeeUsername = notitie.employeeUsername,
            employeeFullname = "${medewerker.firstName} ${medewerker.lastName}",
            // updating a note is only allowed if the logged-in user is the same as the employee who created the note
            updatingAllowed = loggedInUserInstance.get().id == notitie.employeeUsername
        )
    }
}
