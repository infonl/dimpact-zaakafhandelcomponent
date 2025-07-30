package net.atos.zac.flowable.delegate

import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution

fun Expression.resolveValueAsString(execution: DelegateExecution) = this.getValue(execution) as String
