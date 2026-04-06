/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.document.model

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createInboxDocument(
    uuid: UUID = UUID.randomUUID(),
    id: Long = 1L,
    enkelvoudiginformatieobjectID: String = "DOC-123",
    titel: String = "fakeTitel",
    creatiedatum: LocalDate = LocalDate.now(),
    bestandsnaam: String = "test.pdf",
) = InboxDocument().apply {
    this.id = id
    enkelvoudiginformatieobjectUUID = uuid
    this.enkelvoudiginformatieobjectID = enkelvoudiginformatieobjectID
    this.titel = titel
    this.creatiedatum = creatiedatum
    this.bestandsnaam = bestandsnaam
}

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
