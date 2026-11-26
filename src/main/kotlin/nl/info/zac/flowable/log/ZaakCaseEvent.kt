/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.log

import nl.info.zac.log.LogEvent
import java.util.UUID

sealed class ZaakCaseEvent(description: String, val zaakUuid: UUID, val caseDefinitionKey: String) : LogEvent(description) {
    class CmmnModelNotFound(zaakUuid: UUID, caseDefinitionKey: String) :
        ZaakCaseEvent("CMMN model for zaak could not be found; zaak is not started", zaakUuid, caseDefinitionKey)

    class ZaakCaseStartRequested(zaakUuid: UUID, caseDefinitionKey: String) :
        ZaakCaseEvent("Start of the zaak with a CMMN model requested", zaakUuid, caseDefinitionKey)
}
