/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.test.listener

import io.kotest.core.listeners.BeforeContainerListener
import io.kotest.core.test.TestCase
import io.mockk.clearAllMocks

class MockkClearingTestListener : BeforeContainerListener {
    companion object {
        private const val CONTEXT_NAME_PREFIX = "Context:"
    }

    override suspend fun beforeContainer(testCase: TestCase) {
        val isRootLevelGiven = testCase.parent == null
        val isGivenInsideRootContext = testCase.parent?.let {
            it.parent == null && it.name.prefix?.startsWith(CONTEXT_NAME_PREFIX) == true
        } ?: false

        if (isRootLevelGiven || isGivenInsideRootContext) {
            clearAllMocks()
        }
    }
}
