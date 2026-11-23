/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent

class CaseVariableRead(
    val caseInstanceId: String,
    val variable: String,
    val isPresent: Boolean
) : LogEvent("Case variable read")
