/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakCaseEvent(
    val type: Type,
    val zaakUuid: UUID,
    val caseDefinitionKey: String
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        CMMN_MODEL_NOT_FOUND("CMMN model for zaak could not be found; zaak is not started"),
        ZAAK_CASE_START_REQUESTED("Start of the zaak with a CMMN model requested")
    }
}
