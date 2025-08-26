/*
* SPDX-FileCopyrightText: 2025 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.database.flyway

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import nl.info.zac.database.flyway.exception.DatabaseConfigurationException

class FlywayIntegratorTest : BehaviorSpec({
    val flywayIntegrator = FlywayIntegrator()

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A data source that is not initialized") {
        When("onStartup is called") {
            Then("it should throw DatabaseConfigurationException") {
                shouldThrow<DatabaseConfigurationException> {
                    flywayIntegrator.onStartup(Any())
                }
            }
        }
    }
})
