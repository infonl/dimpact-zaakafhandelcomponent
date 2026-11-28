/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent
import java.time.LocalDate

class BrondatumSet(
    val brondatum: LocalDate,
    val zaakIdentificatie: String?
) : LogEvent("Set brondatum for zaak")
