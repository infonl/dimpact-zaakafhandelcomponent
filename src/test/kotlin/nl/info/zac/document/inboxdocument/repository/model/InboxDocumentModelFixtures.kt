/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.repository.model

import nl.info.zac.search.model.DatumRange
import java.time.LocalDate
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

fun createInboxDocumentListParameters(
    title: String? = "fakeTitle",
    identification: String? = "fakeIdentification",
    creationDateRange: DatumRange? = null
) = InboxDocumentListParameters().apply {
    titel = title
    identificatie = identification
    creatiedatum = creationDateRange
}
