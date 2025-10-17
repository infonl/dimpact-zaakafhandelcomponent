/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.note

import nl.info.zac.note.model.Note
import java.time.ZonedDateTime
import java.util.UUID
import java.util.UUID.randomUUID

fun createNote(
    id: Long = 1L,
    zaakUUID: UUID = randomUUID(),
    text: String = "fakeText",
    dateTimeLastModified: ZonedDateTime = ZonedDateTime.now(),
    employeeUsername: String = "fakeUsername",
) = Note().apply {
    this.id = id
    this.zaakUUID = zaakUUID
    this.text = text
    this.dateTimeLastModified = dateTimeLastModified
    this.employeeUsername = employeeUsername
}
