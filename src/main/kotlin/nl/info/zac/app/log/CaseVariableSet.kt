/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent

class CaseVariableSet(
    val caseInstanceId: String,
    val variable: String
) : LogEvent("Case variable set")
