/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import java.util.TimeZone

/**
 * Runs a spec with the JVM default time zone UTC, like the integration test stack, and restores the original zone
 * afterwards.
 */
class UtcDefaultTimeZone : BeforeSpecListener, AfterSpecListener {
    private lateinit var originalTimeZone: TimeZone

    override suspend fun beforeSpec(spec: Spec) {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    override suspend fun afterSpec(spec: Spec) {
        TimeZone.setDefault(originalTimeZone)
    }
}
