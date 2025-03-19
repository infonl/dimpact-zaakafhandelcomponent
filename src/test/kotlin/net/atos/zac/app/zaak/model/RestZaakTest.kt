/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import jakarta.validation.ValidatorFactory

class RestZaakTest : BehaviorSpec({

    Given("RestZaak with blank communication channel data") {
        val restZaak = createRestZaak(communicatiekanaal = " ")

        When("validating the object") {
            val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
            val constraintViolations = factory.validator.validate(restZaak)

            Then("it should return correct constraint violations") {
                with(constraintViolations) {
                    size shouldBe 1
                    with(first()) {
                        propertyPath.toString() shouldBe "communicatiekanaal"
                        message shouldBe "must not be blank"
                        invalidValue shouldBe " "
                    }
                }
            }
        }
    }
})
