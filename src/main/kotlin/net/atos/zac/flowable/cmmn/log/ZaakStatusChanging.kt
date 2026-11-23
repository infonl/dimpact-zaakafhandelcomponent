/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakStatusChanging(
    val zaakUuid: UUID,
    val statustypeOmschrijving: String
) : LogEvent("Changing the status of the zaak")
