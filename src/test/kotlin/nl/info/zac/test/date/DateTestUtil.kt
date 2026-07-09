/*
* SPDX-FileCopyrightText: 2025 Lifely
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.test.date

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
