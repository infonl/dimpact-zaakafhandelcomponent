/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import java.time.ZonedDateTime

/**
 * A half-open time range: [from] is inclusive and [until] is exclusive. A `null` bound means no bound.
 */
data class TimeRange(val from: ZonedDateTime?, val until: ZonedDateTime?)
