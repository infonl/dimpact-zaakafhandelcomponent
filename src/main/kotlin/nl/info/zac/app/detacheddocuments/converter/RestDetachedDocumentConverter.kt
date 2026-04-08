/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments.converter

import jakarta.inject.Inject
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocument
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.document.detacheddocument.model.DetachedDocument
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
    fun convert(document: DetachedDocument, informatieobjectTypeUUID: UUID): RestDetachedDocument {
        val lock = lockService.findLock(document.documentUUID)
        return RestDetachedDocument(
            id = document.id!!,
            documentUUID = document.documentUUID,
            documentID = document.documentID,
            informatieobjectTypeUUID = informatieobjectTypeUUID,
            titel = document.titel,
            zaakID = document.zaakID,
            creatiedatum = document.creatiedatum,
            bestandsnaam = document.bestandsnaam,
            ontkoppeldDoor = userConverter.convertUserId(document.ontkoppeldDoor),
            ontkoppeldOp = document.ontkoppeldOp,
            reden = document.reden,
            isVergrendeld = lock != null && lock.lock != null
        )
    }

    fun convert(documenten: List<DetachedDocument>, informatieobjectTypeUUIDs: List<UUID>): List<RestDetachedDocument> =
        documenten.indices.map { index -> convert(documenten[index], informatieobjectTypeUUIDs[index]) }
}
