/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.document.detacheddocument.model

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

fun createDetachedDocument(
    uuid: UUID = UUID.randomUUID(),
    userId: String? = null,
) = DetachedDocument().apply {
    id = 1L
    documentUUID = uuid
    documentID = "DOC-456"
    titel = "fakeTitel"
    zaakID = "ZAAK-001"
    creatiedatum = LocalDate.now()
    bestandsnaam = "test.pdf"
    ontkoppeldDoor = userId
    ontkoppeldOp = ZonedDateTime.now()
    reden = "fakeReason"
}
