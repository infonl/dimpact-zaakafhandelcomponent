/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.converter

import jakarta.inject.Inject
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocument
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
class RestDetachedDocumentConverter @Inject constructor(
    private val userConverter: RestUserConverter,
    private val lockService: EnkelvoudigInformatieObjectLockService
) {
    fun convert(detachedDocument: DetachedDocument, informatieobjectTypeUUID: UUID): RestDetachedDocument {
        val lock = lockService.findLock(detachedDocument.documentUUID)
        return RestDetachedDocument(
            // conversion is always done from an existing detached document (in the database),
            // so id is always present
            id = detachedDocument.id!!,
            documentUUID = detachedDocument.documentUUID,
            documentID = detachedDocument.documentID,
            informatieobjectTypeUUID = informatieobjectTypeUUID,
            titel = detachedDocument.titel,
            zaakID = detachedDocument.zaakID,
            creatiedatum = detachedDocument.creatiedatum,
            bestandsnaam = detachedDocument.bestandsnaam,
            ontkoppeldDoor = userConverter.convertUserId(detachedDocument.ontkoppeldDoor),
            ontkoppeldOp = detachedDocument.ontkoppeldOp,
            reden = detachedDocument.reden,
            isVergrendeld = lock != null && lock.lock != null
        )
    }

    fun convert(documenten: List<DetachedDocument>, informatieobjectTypeUUIDs: List<UUID>): List<RestDetachedDocument> =
        documenten.indices.map { index -> convert(documenten[index], informatieobjectTypeUUIDs[index]) }
}
