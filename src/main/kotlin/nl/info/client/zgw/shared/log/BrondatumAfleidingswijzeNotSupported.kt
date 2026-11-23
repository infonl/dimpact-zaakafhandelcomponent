/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.log

import nl.info.zac.log.LogEvent
import java.time.LocalDate

class BrondatumAfleidingswijzeNotSupported(
    val brondatum: LocalDate?,
    val afleidingswijze: String?,
    val zaakIdentificatie: String?
) : LogEvent("Failed to set brondatum for afleidingswijze brondatum")
