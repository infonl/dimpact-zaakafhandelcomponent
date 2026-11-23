/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class SmartDocumentsAttendedDepositCreated(
    val isWizardAuthEnabled: Boolean,
    val isFixedUserName: Boolean,
    val employee: Employee?
) : LogEvent("SmartDocuments attended document creation started")
