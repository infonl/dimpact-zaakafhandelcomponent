/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.log

import nl.info.zac.log.LogEvent

class ZakenReleased(
    val zaakCount: Int,
    val screenEventResourceId: String?
) : LogEvent("Released zaken")
