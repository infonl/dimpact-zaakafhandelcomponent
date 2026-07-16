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

    afterEach {
        checkUnnecessaryStub()
    }

    given("A data source that is not initialized") {
        `when`("onStartup is called") {
            then("it should throw DatabaseConfigurationException") {
                shouldThrow<DatabaseConfigurationException> {
                    flywayIntegrator.onStartup(Any())
                }
            }
        }
    }
})
