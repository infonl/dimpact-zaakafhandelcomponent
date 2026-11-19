/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository.model

import nl.info.zac.time.ZacTime
import java.util.UUID

fun createDetachedDocument(
    uuid: UUID = UUID.randomUUID(),
    username: String = "fakeUsername",
) = DetachedDocument().apply {
    id = 1L
    documentUUID = uuid
    documentID = "DOC-456"
    titel = "fakeTitel"
    zaakID = "ZAAK-001"
    creatiedatum = ZacTime.today()
    bestandsnaam = "test.pdf"
    ontkoppeldDoor = username
    ontkoppeldOp = ZacTime.now()
    reden = "fakeReason"
}
