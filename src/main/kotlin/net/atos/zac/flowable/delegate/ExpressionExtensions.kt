/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

fun Expression.resolveValueAsString(execution: DelegateExecution) = this.getValue(execution) as String
