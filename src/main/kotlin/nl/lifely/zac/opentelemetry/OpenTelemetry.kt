package nl.lifely.zac.opentelemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

fun startOpenTelemetrySpanWithoutParent(tracer: Tracer, spanName: String): Span =
    tracer.spanBuilder(spanName).setNoParent().startSpan().let {
        it.makeCurrent()
        it
    }
