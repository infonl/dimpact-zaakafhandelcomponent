package nl.lifely.zac.test.listener

import io.kotest.core.annotation.AutoScan
import io.kotest.core.listeners.BeforeContainerListener
import io.kotest.core.test.TestCase
import io.mockk.clearAllMocks

@AutoScan
class MockkClearingTestListener : BeforeContainerListener {
    override suspend fun beforeContainer(testCase: TestCase) {
        // only run before Given
        if (testCase.parent == null) {
            clearAllMocks()
        }
    }
}
