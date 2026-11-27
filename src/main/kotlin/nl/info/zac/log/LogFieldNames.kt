/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

/**
 * The standard names of log fields for concepts that more than one [LogEvent] names, so that the same concept has the
 * same field name in every log record.
 */
object LogFieldNames {
    const val EVENT = "event"
    const val ZAAK_UUID = "zaakUuid"
    const val ZAAK_IDENTIFICATIE = "zaakIdentificatie"
    const val ZAAK_URI = "zaakUri"
    const val ZAAKTYPE_UUID = "zaaktypeUuid"
    const val ZAAKTYPE_URI = "zaaktypeUri"
    const val BESLUITTYPE_UUID = "besluittypeUuid"
    const val ROL_UUID = "rolUuid"
    const val TAAK_ID = "taakId"
    const val INFORMATIEOBJECT_UUID = "informatieobjectUuid"
    const val INFORMATIEOBJECT_IDENTIFICATIE = "informatieobjectIdentificatie"
    const val GROUP_ID = "groupId"
    const val EMPLOYEE = "employee"
    const val REQUEST_ID = "requestId"
    const val PROCESS_DEFINITION_KEY = "processDefinitionKey"
    const val CASE_DEFINITION_KEY = "caseDefinitionKey"
    const val OUTCOME = "outcome"
    const val DURATION_MS = "durationMs"
    const val ERROR_TYPE = "errorType"
    const val ERROR_CODE = "errorCode"
    const val TRACE_ID = "traceId"
    const val SPAN_ID = "spanId"
}
