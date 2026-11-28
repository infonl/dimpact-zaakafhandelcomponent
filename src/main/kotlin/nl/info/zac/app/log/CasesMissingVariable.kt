/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent

class CasesMissingVariable(
    val variable: String,
    val caseCount: Long
) : LogEvent("Number of cases missing variable")
