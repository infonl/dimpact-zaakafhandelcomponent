/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.model

import net.atos.zac.documenten.model.OntkoppeldDocument
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

fun createOntkoppeldDocument(
    uuid: UUID = UUID.randomUUID(),
    userId: String? = null,
) = OntkoppeldDocument().apply {
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
