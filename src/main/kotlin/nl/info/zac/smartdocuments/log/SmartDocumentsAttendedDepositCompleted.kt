/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent
import nl.info.zac.log.Outcome

@Suppress("LongParameterList")
class SmartDocumentsAttendedDepositCompleted(
    val isWizardAuthEnabled: Boolean,
    val isFixedUserName: Boolean,
    val employee: Employee?,
    val outcome: Outcome,
    val durationMs: Long,
    val errorType: String?
) : LogEvent("SmartDocuments attended document creation completed")
