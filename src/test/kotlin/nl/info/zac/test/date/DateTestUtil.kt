/*
* SPDX-FileCopyrightText: 2025 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.test.date

import nl.info.zac.time.ZacTime
import java.time.LocalDate
import java.util.Date

fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZacTime.ZONE).toInstant())
