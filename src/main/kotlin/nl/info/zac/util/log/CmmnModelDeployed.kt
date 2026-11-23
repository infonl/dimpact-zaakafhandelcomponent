/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.log

import nl.info.zac.log.LogEvent

class CmmnModelDeployed(
    val caseDefinitionKey: String,
    val name: String,
    val modelFileName: String
) : LogEvent("Successfully deployed CMMN model")
