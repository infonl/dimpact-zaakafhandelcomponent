/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.delegate.log

import nl.info.zac.log.LogEvent
import java.time.ZonedDateTime

class ZaakResumptionStarted(
    val zaakIdentificatie: String?,
    val activityName: String?,
    val resumeDate: ZonedDateTime?
) : LogEvent("Zaak resumption started")
