package nl.lifely.zac.opentelemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

fun startOpenTelemetrySpanWithoutParent(tracer: Tracer, spanName: String): Span {
    val span = tracer.spanBuilder(spanName).setNoParent().startSpan()
    span.makeCurrent()
    return span
}
