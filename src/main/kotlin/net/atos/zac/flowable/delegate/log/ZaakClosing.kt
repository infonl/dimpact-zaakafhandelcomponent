/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakClosing(
    val zaakUuid: UUID,
    val resultaattypeOmschrijving: String
) : LogEvent("Closing zaak")
