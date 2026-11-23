/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakProcessStarting(
    val zaakUuid: UUID,
    val processDefinitionKey: String
) : LogEvent("Starting zaak using BPMN model")
