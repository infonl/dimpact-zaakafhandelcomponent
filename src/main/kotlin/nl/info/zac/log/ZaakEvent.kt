/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

sealed class ZaakEvent(description: String, val zaakUuid: UUID) : LogEvent(description) {
    class FlowableProcessDataDeleted(zaakUuid: UUID) : ZaakEvent("Deleted Flowable process data for zaak", zaakUuid)

    class MailPdfConversionFailed(zaakUuid: UUID) :
        ZaakEvent("Failed to convert the sent e-mail to PDF; no zaak document was created", zaakUuid)

    class ZaakOfDeletedRolNotFound(zaakUuid: UUID) :
        ZaakEvent("Zaak of a deleted rol no longer exists; no signaleringen are deleted", zaakUuid)

    class ZaakeigenschapReadFailed(zaakUuid: UUID) :
        ZaakEvent("Failed to read zaakeigenschap; reindexing the zaak defensively", zaakUuid)
}
