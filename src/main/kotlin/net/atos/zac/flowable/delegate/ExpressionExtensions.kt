/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.math.BigDecimal
import java.time.ZonedDateTime

fun Expression.resolveValueAsString(execution: DelegateExecution) =
    this.getValue(execution) as String

fun Expression.resolveValueAsLong(execution: DelegateExecution): Long =
    this.getValue(execution).let {
        when (it) {
            is BigDecimal -> it.toLong()
            is String -> it.toLong()
            else -> it as Long
        }
    }

fun Expression.resolveValueAsZonedDateTime(execution: DelegateExecution): ZonedDateTime =
    ZonedDateTime.parse(this.getValue(execution) as String)
