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
    fun convertToRestDetachedDocument(
        detachedDocument: DetachedDocument,
        informatieobjectTypeUUID: UUID,
        isZaakspecifiekGeautoriseerd: Boolean
    ): RestDetachedDocument {
        val lock = lockService.findLock(detachedDocument.documentUUID)
        return RestDetachedDocument(
            id = checkNotNull(detachedDocument.id) {
                "Detached document '${detachedDocument.documentUUID}' has not been persisted"
            },
            documentUUID = detachedDocument.documentUUID,
            documentID = detachedDocument.documentID,
            informatieobjectTypeUUID = informatieobjectTypeUUID,
            titel = detachedDocument.titel,
            zaakID = detachedDocument.zaakID,
            isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd,
            creatiedatum = detachedDocument.creatiedatum,
            bestandsnaam = detachedDocument.bestandsnaam,
            ontkoppeldDoor = userConverter.convertUsername(
                username = detachedDocument.ontkoppeldDoor,
                logContext = "ontkoppeld document '${detachedDocument.documentID}' of zaak '${detachedDocument.zaakID}'"
            ),
            ontkoppeldOp = detachedDocument.ontkoppeldOp,
            reden = detachedDocument.reden,
            isVergrendeld = lock != null && lock.lock != null
        )
    }

    fun convertToRestDetachedDocuments(
        documenten: List<DetachedDocument>,
        informatieobjectTypeUUIDs: List<UUID>,
        zaakspecifiekGeautoriseerdeZaakIDs: Set<String>
    ): List<RestDetachedDocument> =
        documenten.indices.map { index ->
            convertToRestDetachedDocument(
                detachedDocument = documenten[index],
                informatieobjectTypeUUID = informatieobjectTypeUUIDs[index],
                isZaakspecifiekGeautoriseerd = documenten[index].zaakID in zaakspecifiekGeautoriseerdeZaakIDs
            )
        }
}
