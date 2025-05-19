/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.note

import nl.info.zac.app.note.model.RestNote
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createRestNote(
    id: Long = 1,
    zaakUUID: UUID = UUID.randomUUID(),
    text: String = "fakeText",
    dateTimeLastModified: ZonedDateTime = ZonedDateTime.now(),
    employeeUsername: String = "fakeUserName",
    employeeFullname: String = "fakeUserFullName",
    updatingAllowed: Boolean = true
) = RestNote(
    id = id,
    zaakUUID = zaakUUID,
    text = text,
    dateTimeLastModified = dateTimeLastModified,
    employeeUsername = employeeUsername,
    employeeFullname = employeeFullname,
    updatingAllowed = updatingAllowed
)
