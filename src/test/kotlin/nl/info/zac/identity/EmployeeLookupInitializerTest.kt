/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.info.zac.log.Employee
import nl.info.zac.log.EmployeeKeycloakUserIds
import nl.info.zac.log.FakeEmployeeAction
import nl.info.zac.log.LogCapture
import nl.info.zac.log.ZacLogger
import java.util.UUID

class EmployeeLookupInitializerTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val employeeLookupInitializer = EmployeeLookupInitializer(identityService)

    afterEach {
        checkUnnecessaryStub()
    }

    afterSpec {
        EmployeeKeycloakUserIds.reset()
    }

    context("Starting the application") {
        given("an employee whom Keycloak knows and who has not logged in") {
            every { identityService.findKeycloakUserId("fakeLookupUsername") } returns "fakeKeycloakUserId"

            `when`("the application starts and an event with that employee is logged") {
                employeeLookupInitializer.onStartup(Any())
                val records = LogCapture(EmployeeLookupInitializerTest::class).use { logCapture ->
                    ZacLogger.getLogger(EmployeeLookupInitializerTest::class)
                        .info(FakeEmployeeAction(zaakUuid = UUID.randomUUID(), employee = Employee("fakeLookupUsername")))
                    logCapture.records
                }

                then("the employee is written as the Keycloak user ID that the identity service finds") {
                    records.single().mdc["employee"] shouldBe "fakeKeycloakUserId"
                    verify(exactly = 1) { identityService.findKeycloakUserId("fakeLookupUsername") }
                }
            }
        }
    }
})
