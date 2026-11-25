/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import org.jboss.logging.MDC
import java.util.UUID
import java.util.logging.Level

class ZacLoggerTest : BehaviorSpec({
    val zacLogger = ZacLogger.getLogger(ZacLoggerTest::class)
    val zaakUuid = UUID.fromString("5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f")

    beforeEach {
        EmployeeKeycloakUserIds.reset()
        MDC.clear()
    }

    context("Logging an event") {
        given("an event with a zaak and a group") {
            `when`("it is logged at INFO") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                    logCapture.records
                }

                then("one record is written with the description as message and each field as an MDC entry") {
                    records shouldHaveSize 1
                    with(records.single()) {
                        level shouldBe Level.INFO
                        message shouldBe "Zaak assigned"
                        mdc shouldBe mapOf(
                            "event" to "FakeZaakAssigned",
                            "zaakIdentificatie" to "ZAAK-2026-0000000001",
                            "groupId" to "test-group"
                        )
                    }
                }
            }
        }

        given("an event with a redacted recipient") {
            `when`("it is logged") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.fine(
                        FakeMailSendCompleted(
                            zaakUuid = zaakUuid,
                            recipient = "burger@example.com",
                            outcome = Outcome.SUCCESS,
                            durationMs = 120
                        )
                    )
                    logCapture.records
                }

                then("the recipient is masked and its value is nowhere in the record") {
                    with(records.single()) {
                        message shouldBe "Mail send completed"
                        mdc shouldContainAll mapOf(
                            "recipient" to REDACTED_VALUE,
                            "outcome" to "SUCCESS",
                            "durationMs" to "120"
                        )
                        containsAnywhere("burger@example.com") shouldBe false
                    }
                }
            }
        }

        given("an event with a null redacted field") {
            `when`("it is logged") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.info(
                        FakeMailSendCompleted(zaakUuid = zaakUuid, recipient = null, outcome = Outcome.FAILURE, durationMs = 0)
                    )
                    logCapture.records
                }

                then("the redacted field is written as null") {
                    records.single().mdc["recipient"] shouldBe "null"
                }
            }
        }

        given("an exception") {
            val illegalStateException = IllegalStateException("fakeExceptionMessage")

            `when`("an event is logged at SEVERE with the exception") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.severe(
                        FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"),
                        illegalStateException
                    )
                    logCapture.records
                }

                then("the record has the event, its fields, the exception and its class name as error type") {
                    with(records.single()) {
                        level shouldBe Level.SEVERE
                        thrown shouldBe illegalStateException
                        mdc shouldContainAll mapOf(
                            "zaakIdentificatie" to "ZAAK-2026-0000000001",
                            "errorType" to "java.lang.IllegalStateException"
                        )
                    }
                }
            }
        }

        given("a logger for which FINE is disabled") {
            var isEventCreated = false
            var isEmployeeLookedUp = false
            ZacLogger.useEmployeeLookup {
                isEmployeeLookedUp = true
                "fakeKeycloakUserId"
            }

            `when`("an event is logged at FINE, directly and through a supplier") {
                val records = LogCapture(ZacLoggerTest::class, Level.INFO).use { logCapture ->
                    zacLogger.fine(FakeEmployeeAction(zaakUuid = zaakUuid, employee = Employee("fakeUsername")))
                    zacLogger.fine {
                        isEventCreated = true
                        FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group")
                    }
                    logCapture.records
                }

                then("no record is written, the supplier is not called and the employee is not looked up") {
                    records.shouldBeEmpty()
                    isEventCreated shouldBe false
                    isEmployeeLookedUp shouldBe false
                }
            }
        }

        given("an active span") {
            val spanContext = SpanContext.create(
                "0af7651916cd43dd8448eb211c80319c",
                "b7ad6b7169203331",
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )

            `when`("an event is logged inside the span") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    Span.wrap(spanContext).makeCurrent().use {
                        zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                    }
                    logCapture.records
                }

                then("the record has the trace ID and the span ID of the span") {
                    records.single().mdc shouldContainAll mapOf(
                        "traceId" to "0af7651916cd43dd8448eb211c80319c",
                        "spanId" to "b7ad6b7169203331"
                    )
                }
            }
        }

        given("no active span") {
            `when`("an event is logged") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                    logCapture.records
                }

                then("the record has neither a trace ID nor a span ID") {
                    with(records.single().mdc) {
                        this shouldNotContainKey "traceId"
                        this shouldNotContainKey "spanId"
                    }
                }
            }
        }

        given("an MDC entry of the caller") {
            `when`("an event is logged, and then another event without that event's fields") {
                MDC.put("fakeCallerKey", "fakeCallerValue")
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.info(FakeEmployeeAction(zaakUuid = zaakUuid, employee = Employee("fakeUsername")))
                    zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                    logCapture.records
                }
                val mdcAfterLogging = MDC.getMap().toMap()

                then("the second record has no field of the first, and the MDC of the caller is restored") {
                    records[1].mdc shouldNotContainKey "zaakUuid"
                    mdcAfterLogging shouldBe mapOf("fakeCallerKey" to "fakeCallerValue")
                }
            }

            `when`("writing the record throws") {
                MDC.put("fakeCallerKey", "fakeCallerValue")
                val illegalStateException = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    logCapture.onPublish = { throw IllegalStateException("fakeHandlerError") }
                    shouldThrow<IllegalStateException> {
                        zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                    }
                }
                val mdcAfterLogging = MDC.getMap().toMap()

                then("the MDC of the caller is still restored") {
                    illegalStateException.message shouldBe "fakeHandlerError"
                    mdcAfterLogging shouldBe mapOf("fakeCallerKey" to "fakeCallerValue")
                }
            }
        }
    }

    context("Logging an event with an employee") {
        given("an employee who logged in on this ZAC instance") {
            var lookupCount = 0
            ZacLogger.useEmployeeLookup {
                lookupCount++
                "fakeOtherKeycloakUserId"
            }
            ZacLogger.registerEmployee("fakeUsername", "fakeKeycloakUserId")

            `when`("an event with that employee is logged") {
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    zacLogger.info(FakeEmployeeAction(zaakUuid = zaakUuid, employee = Employee("fakeUsername")))
                    logCapture.records
                }

                then("the record has the registered Keycloak user ID, without a lookup and without the username") {
                    with(records.single()) {
                        mdc["employee"] shouldBe "fakeKeycloakUserId"
                        containsAnywhere("fakeUsername") shouldBe false
                    }
                    lookupCount shouldBe 0
                }
            }
        }

        given("an employee lookup that logs an event of its own") {
            val lookupLogger = ZacLogger.getLogger(FakeGroupLookup::class)
            ZacLogger.useEmployeeLookup {
                lookupLogger.info(FakeGroupLookup(groupId = "fakeGroupId"))
                "fakeKeycloakUserId"
            }

            `when`("an event with a zaak UUID and an employee is logged") {
                val (outerRecords, innerRecords) = LogCapture(ZacLoggerTest::class).use { outerCapture ->
                    LogCapture(FakeGroupLookup::class).use { innerCapture ->
                        zacLogger.info(FakeEmployeeAction(zaakUuid = zaakUuid, employee = Employee("fakeUsername")))
                        outerCapture.records to innerCapture.records
                    }
                }

                then("neither record has a field of the other") {
                    innerRecords.single().mdc shouldNotContainKey "zaakUuid"
                    innerRecords.single().mdc shouldNotContainKey "employee"
                    innerRecords.single().mdc shouldContainAll mapOf("groupId" to "fakeGroupId")
                    outerRecords.single().mdc shouldNotContainKey "groupId"
                    outerRecords.single().mdc shouldContainAll mapOf(
                        "zaakUuid" to zaakUuid.toString(),
                        "employee" to "fakeKeycloakUserId"
                    )
                }
            }
        }
    }

    context("Logging in a block of a logged-in employee") {
        given("the Keycloak user ID of the logged-in employee") {
            `when`("an event without an employee is logged in the block") {
                lateinit var blockResult: String
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    blockResult = ZacLogger.withLoggedInEmployee("fakeKeycloakUserId") {
                        zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                        "fakeBlockResult"
                    }
                    logCapture.records
                }
                val mdcAfterBlock = MDC.getMap().toMap()

                then("the record has the Keycloak user ID as employee and the block returns its result") {
                    records.single().mdc shouldContainAll mapOf(
                        "employee" to "fakeKeycloakUserId",
                        "zaakIdentificatie" to "ZAAK-2026-0000000001"
                    )
                    blockResult shouldBe "fakeBlockResult"
                }

                and("the employee is removed from the MDC after the block") {
                    mdcAfterBlock shouldNotContainKey "employee"
                }
            }

            `when`("an event with an employee of its own is logged in the block, and then an event without one") {
                ZacLogger.registerEmployee("fakeUsername", "fakeOtherKeycloakUserId")
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    ZacLogger.withLoggedInEmployee("fakeKeycloakUserId") {
                        zacLogger.info(FakeEmployeeAction(zaakUuid = zaakUuid, employee = Employee("fakeUsername")))
                        zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                    }
                    logCapture.records
                }

                then("the first record has the employee of the event and the second the logged-in employee") {
                    records[0].mdc["employee"] shouldBe "fakeOtherKeycloakUserId"
                    records[1].mdc["employee"] shouldBe "fakeKeycloakUserId"
                }
            }

            `when`("the block throws") {
                val illegalStateException = shouldThrow<IllegalStateException> {
                    ZacLogger.withLoggedInEmployee("fakeKeycloakUserId") {
                        throw IllegalStateException("fakeBlockError")
                    }
                }
                val mdcAfterBlock = MDC.getMap().toMap()

                then("the exception reaches the caller and the employee is removed from the MDC") {
                    illegalStateException.message shouldBe "fakeBlockError"
                    mdcAfterBlock shouldNotContainKey "employee"
                }
            }
        }

        given("an employee in the MDC before the block") {
            `when`("an event is logged in a nested block of another logged-in employee") {
                lateinit var employeeAfterInnerBlock: String
                val records = LogCapture(ZacLoggerTest::class).use { logCapture ->
                    ZacLogger.withLoggedInEmployee("fakeOuterKeycloakUserId") {
                        ZacLogger.withLoggedInEmployee("fakeInnerKeycloakUserId") {
                            zacLogger.info(FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "test-group"))
                        }
                        employeeAfterInnerBlock = MDC.get("employee") as String
                    }
                    logCapture.records
                }
                val mdcAfterOuterBlock = MDC.getMap().toMap()

                then("the record has the inner employee and the previous employee is restored after the inner block") {
                    records.single().mdc["employee"] shouldBe "fakeInnerKeycloakUserId"
                    employeeAfterInnerBlock shouldBe "fakeOuterKeycloakUserId"
                    mdcAfterOuterBlock shouldNotContainKey "employee"
                }
            }
        }
    }
})
