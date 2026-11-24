/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class LogEventRendererTest : BehaviorSpec({
    val zaakUuid = UUID.fromString("5f0c8f8e-6f0d-4c63-9d0e-2b8a1c3e4d5f")

    context("Rendering the fields of a log event") {
        given("an event with a redacted recipient") {
            val fakeMailSendCompleted = FakeMailSendCompleted(
                zaakUuid = zaakUuid,
                recipient = "burger@example.com",
                outcome = Outcome.SUCCESS,
                durationMs = 84
            )

            `when`("the fields are rendered") {
                val fields = LogEventRenderer.fields(fakeMailSendCompleted)

                then("each field is written in declaration order, with the recipient masked and the enum by its name") {
                    fields shouldBe linkedMapOf(
                        "zaakUuid" to zaakUuid.toString(),
                        "recipient" to REDACTED_VALUE,
                        "outcome" to "SUCCESS",
                        "durationMs" to "84"
                    )
                    fields.keys.toList() shouldBe listOf("zaakUuid", "recipient", "outcome", "durationMs")
                }
            }
        }

        given("an event whose redacted field and other field are null") {
            val fakeMailSendCompleted = FakeMailSendCompleted(
                zaakUuid = null,
                recipient = null,
                outcome = Outcome.FAILURE,
                durationMs = 0
            )

            `when`("the fields are rendered") {
                val fields = LogEventRenderer.fields(fakeMailSendCompleted)

                then("both are written as null") {
                    fields["zaakUuid"] shouldBe "null"
                    fields["recipient"] shouldBe "null"
                }
            }
        }

        given("an event with an employee") {
            val fakeEmployeeAction = FakeEmployeeAction(zaakUuid = zaakUuid, employee = Employee("fakeUsername"))

            `when`("the fields are rendered without an employee renderer") {
                val fields = LogEventRenderer.fields(fakeEmployeeAction)

                then("the employee is written as unknown") {
                    fields["employee"] shouldBe UNKNOWN_EMPLOYEE
                }
            }

            `when`("the fields are rendered with an employee renderer") {
                val fields = LogEventRenderer.fields(fakeEmployeeAction) { "fakeKeycloakUserId" }

                then("the employee is written as the result of the renderer") {
                    fields["employee"] shouldBe "fakeKeycloakUserId"
                }
            }
        }

        given("an event that inherits a field from an abstract event and has a list field") {
            val fakeDerivedEvent = FakeDerivedEvent(
                zaakIdentificatie = "ZAAK-2026-0000000001",
                objectTypes = listOf(Outcome.SUCCESS, Outcome.FAILURE),
                count = 2
            )

            `when`("the event is rendered as text") {
                val text = LogEventRenderer.render(fakeDerivedEvent)

                then("the inherited field comes first, the list is written with its elements, and the description leads") {
                    text shouldBe "Derived event [zaakIdentificatie=ZAAK-2026-0000000001, objectTypes=[SUCCESS, FAILURE], count=2]"
                }
            }
        }

        given("an event with a static field and an inner event class with a synthetic field of its outer class") {
            val fakeEventWithConstant = FakeEventWithConstant(count = 3)
            val innerEvent = object : LogEvent("Inner event") {
                val count = 3
                val countOfOuterEvent get() = fakeEventWithConstant.count
            }

            `when`("the fields of both events are rendered") {
                val fieldsOfEventWithConstant = LogEventRenderer.fields(fakeEventWithConstant)
                val fieldsOfInnerEvent = LogEventRenderer.fields(innerEvent)

                then("only the fields of the event itself are written") {
                    fieldsOfEventWithConstant shouldBe mapOf("count" to "3")
                    fieldsOfInnerEvent shouldBe mapOf("count" to "3")
                }
            }
        }
    }

    context("Naming a log event") {
        given("an event with a class of its own and an event that shares its class through a type") {
            val fakeZaakAssigned = FakeZaakAssigned(zaakIdentificatie = "ZAAK-2026-0000000001", groupId = "fakeGroupId")
            val genericEvent = GenericEvent(type = GenericEvent.Type.SMTP_AUTHENTICATION_DISABLED)

            `when`("their event names are determined") {
                val eventNames = listOf(fakeZaakAssigned, genericEvent).map(LogEventRenderer::eventName)

                then(
                    "the first is named after its class and the second after its type constant in the same style, " +
                        "so that one field identifies every event"
                ) {
                    eventNames shouldBe listOf("FakeZaakAssigned", "SmtpAuthenticationDisabled")
                }
            }
        }
    }

    context("Writing an employee as text") {
        given("an employee") {
            val employee = Employee("fakeUsername")

            `when`("it is converted to a string") {
                val text = employee.toString()

                then("the username is not in the text") {
                    text shouldBe "Employee(username=$REDACTED_VALUE)"
                }
            }
        }
    }
})
