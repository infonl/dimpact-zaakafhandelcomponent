/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.opentelemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Runs a block of code in an Open Telemetry span with the given name and parameters.
 * Taken from: https://danielcorreia.net/opentelemetry-kotlin-coroutines/
 */
suspend fun <T> withSpan(
    tracer: Tracer,
    spanName: String,
    parameters: (SpanBuilder.() -> Unit)? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (span: Span) -> T
): T = tracer.startSpan(spanName, parameters, coroutineContext, block)

/**
 * Starts a new Open Telemtry span with the given name and parameters, executes the given block of code,
 * record any exceptions and rethrows them, and ends the span.
 * Taken from: https://danielcorreia.net/opentelemetry-kotlin-coroutines/
 */
@Suppress("TooGenericExceptionCaught")
suspend fun <T> Tracer.startSpan(
    spanName: String,
    parameters: (SpanBuilder.() -> Unit)? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (span: Span) -> T
): T {
    val span: Span = this.spanBuilder(spanName).run {
        if (parameters != null) parameters()
        startSpan()
    }
    return withContext(coroutineContext + span.asContextElement()) {
        try {
            block(span)
        } catch (throwable: Throwable) {
            span.setStatus(StatusCode.ERROR)
            span.recordException(throwable)
            throw throwable
        } finally {
            span.end()
        }
    }
}
