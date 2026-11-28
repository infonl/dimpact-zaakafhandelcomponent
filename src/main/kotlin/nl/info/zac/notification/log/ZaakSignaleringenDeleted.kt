/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ZaakSignaleringenDeleted(
    val zaakUuid: UUID,
    val zaakSignaleringCount: Int,
    val isZaakSignaleringVerzondenDeleted: Boolean,
    val taakSignaleringCount: Int,
    val taakSignaleringVerzondenCount: Int
) : LogEvent("Deleted signaleringen for zaak")
