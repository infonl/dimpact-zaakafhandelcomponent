/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.model

import net.atos.zac.documenten.model.InboxDocument
import java.time.LocalDate
import java.util.UUID

fun createInboxDocument(
    uuid: UUID = UUID.randomUUID()
) = InboxDocument().apply {
    id = 1L
    enkelvoudiginformatieobjectUUID = uuid
    enkelvoudiginformatieobjectID = "DOC-123"
    titel = "fakeTitel"
    creatiedatum = LocalDate.now()
    bestandsnaam = "test.pdf"
}
