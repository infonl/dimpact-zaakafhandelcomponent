/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.architecture.fixtures

import java.util.logging.Logger

class JulLoggerFixture {
    fun log() = Logger.getLogger(JulLoggerFixture::class.java.name).info("fakeMessage")
}
