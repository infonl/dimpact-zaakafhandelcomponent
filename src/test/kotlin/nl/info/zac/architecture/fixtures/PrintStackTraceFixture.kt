/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.architecture.fixtures

class PrintStackTraceFixture {
    fun log() = IllegalStateException("fakeMessage").printStackTrace()
}
