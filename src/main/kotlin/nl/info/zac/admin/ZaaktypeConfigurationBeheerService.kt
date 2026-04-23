/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
class ZaaktypeConfigurationBeheerService @Inject constructor(
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService
) {
    fun mapSmartDocuments(previousZaaktypeUuid: UUID, newZaaktypeUuid: UUID) {
        val templateMappings = smartDocumentsTemplatesService.getTemplatesMapping(previousZaaktypeUuid)
        smartDocumentsTemplatesService.storeTemplatesMapping(templateMappings, newZaaktypeUuid)
    }
}
