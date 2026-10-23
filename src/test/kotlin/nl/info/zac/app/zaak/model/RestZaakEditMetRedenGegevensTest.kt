/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class RestZaakEditMetRedenGegevensTest : BehaviorSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    context("Validation of RestZaakEditMetRedenGegevens") {
        given("a zaak change with a reason") {
            val restZaakEditMetRedenGegevens = RestZaakEditMetRedenGegevens(
                zaak = createRestZaakCreateData(),
                reden = "fakeReason"
            )

            `when`("validating the zaak change") {
                val violations = validator.validate(restZaakEditMetRedenGegevens)

                then("there are no constraint violations") {
                    violations.shouldBeEmpty()
                }
            }
        }

        listOf("", " ").forEach { blankReason ->
            given("a zaak change with the blank reason '$blankReason'") {
                val restZaakEditMetRedenGegevens = RestZaakEditMetRedenGegevens(
                    zaak = createRestZaakCreateData(),
                    reden = blankReason
                )

                `when`("validating the zaak change") {
                    val violations = validator.validate(restZaakEditMetRedenGegevens)

                    then("there is one constraint violation on the reason, because the reason is required") {
                        violations shouldHaveSize 1
                        violations.first().propertyPath.toString() shouldBe "reden"
                    }
                }
            }
        }
    }
})
