/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.test.listener

import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.mockk.checkUnnecessaryStub

class UnnecessaryStubCheckingTestListener : AfterEachListener {
    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        checkUnnecessaryStub()
    }
}
