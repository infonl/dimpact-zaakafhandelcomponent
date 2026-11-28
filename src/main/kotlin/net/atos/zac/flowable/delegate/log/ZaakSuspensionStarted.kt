/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.LogEvent

class ZaakSuspensionStarted(
    val zaakIdentificatie: String?,
    val activityName: String?,
    val numberOfDays: Long
) : LogEvent("Zaak suspension started")
