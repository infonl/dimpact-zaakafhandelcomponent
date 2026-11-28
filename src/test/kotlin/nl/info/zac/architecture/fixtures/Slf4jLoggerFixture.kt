/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.architecture.fixtures

import org.slf4j.LoggerFactory

class Slf4jLoggerFixture {
    fun log() = LoggerFactory.getLogger(Slf4jLoggerFixture::class.java).info("fakeMessage")
}
