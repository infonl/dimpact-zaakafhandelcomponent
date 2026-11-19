/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import java.time.ZonedDateTime

/**
 * A half-open time range: a start is inclusive and an end is exclusive. The type says which bounds the range has.
 */
sealed interface TimeRange {
    data class Between(val from: ZonedDateTime, val until: ZonedDateTime) : TimeRange

    data class From(val from: ZonedDateTime) : TimeRange

    data class Until(val until: ZonedDateTime) : TimeRange
}
