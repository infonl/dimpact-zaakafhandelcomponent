/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

sealed class ZaaktypeEvent(description: String, val zaaktypeUuid: UUID) : LogEvent(description) {
    class ConceptZaaktypeIgnored(zaaktypeUuid: UUID, val zaaktypeOmschrijving: String) :
        ZaaktypeEvent("Zaaktype is still a concept; ignoring it", zaaktypeUuid)

    class ExistingZaaktypeConfigurationFound(zaaktypeUuid: UUID) :
        ZaaktypeEvent("Zaaktype configuration already exists; updating it", zaaktypeUuid)

    class TemplateMappingStorageStarted(zaaktypeUuid: UUID) :
        ZaaktypeEvent("Storage of the template mapping for zaaktype started", zaaktypeUuid)

    class ZaaktypeWithoutConfigurationIgnored(zaaktypeUuid: UUID, val zaaktypeOmschrijving: String) :
        ZaaktypeEvent("Zaaktype has no known configuration; ignoring it", zaaktypeUuid)
}
