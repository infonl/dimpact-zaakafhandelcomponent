/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.log

import nl.info.zac.log.LogEvent

class UnparseableModificationDate(
    val processDefinitionKey: String,
    val modificationDate: String
) : LogEvent("Ignoring unparseable modification date in BPMN process definition")
