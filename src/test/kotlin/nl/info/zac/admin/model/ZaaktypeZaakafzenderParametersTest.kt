/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class ZaaktypeZaakafzenderParametersTest : BehaviorSpec({
    val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

    context("equals") {
        given("a set with a zaakafzender whose replyTo changed after it was added to the set") {
            val zaakafzenderParameters = createZaakAfzender(
                replyTo = "old@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenders = setOf(zaakafzenderParameters)
            zaakafzenderParameters.replyTo = "new@example.com"

            `when`("the set is checked for an equal zaakafzender") {
                val isFound = createZaakAfzender(
                    replyTo = "new@example.com",
                    zaaktypeConfiguration = zaaktypeCmmnConfiguration
                ) in zaakafzenders

                then("the set finds it, because the hash code does not depend on the changed field") {
                    isFound shouldBe true
                }
            }
        }

        given("Two equal objects") {
            val zaakafzenderParameters1 = createZaakAfzender(zaaktypeConfiguration = zaaktypeCmmnConfiguration)
            val zaakafzenderParameters2 = createZaakAfzender(zaaktypeConfiguration = zaaktypeCmmnConfiguration)

            `when`("they are compared") {
                val isEqual = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be equal") {
                    isEqual shouldBe true
                }

                and("they should have the same hashcode") {
                    hashCode1 shouldBe hashCode2
                }
            }
        }

        given("A zaakafzender and an object of another type") {
            val zaakafzenderParameters = createZaakAfzender(zaaktypeConfiguration = zaaktypeCmmnConfiguration)

            `when`("they are compared") {
                val isEqual = zaakafzenderParameters.equals("fakeMail")

                then("they should be different") {
                    isEqual shouldBe false
                }
            }
        }

        given("Two objects differ in the defaultMail property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                defaultMail = true,
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                defaultMail = false,
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val isEqual = zaakafzenderParameters1.equals(zaakafzenderParameters2)

                then("they should be different") {
                    isEqual shouldBe false
                }
            }
        }

        given("Two objects differ in the mail property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                mail = "mail1@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                mail = "mail2@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val isEqual = zaakafzenderParameters1.equals(zaakafzenderParameters2)

                then("they should be different") {
                    isEqual shouldBe false
                }
            }
        }

        given("Two objects differ in the replyTo property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                replyTo = "mail1@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                replyTo = "mail2@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val isEqual = zaakafzenderParameters1.equals(zaakafzenderParameters2)

                then("they should be different") {
                    isEqual shouldBe false
                }
            }
        }

        given("Two objects differ in multiple properties") {
            val zaakafzenderParameters1 = createZaakAfzender(
                defaultMail = true,
                replyTo = "mail1@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                defaultMail = false,
                replyTo = "mail2@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val isEqual = zaakafzenderParameters1.equals(zaakafzenderParameters2)

                then("they should be different") {
                    isEqual shouldBe false
                }
            }
        }
    }

    context("validation") {
        val validator = Validation.buildDefaultValidatorFactory().validator

        given("a valid zaakafzender") {
            val zaakafzenderParameters = createZaakAfzender(zaaktypeConfiguration = zaaktypeCmmnConfiguration)

            `when`("validating the zaakafzender") {
                val validationResult = validator.validate(zaakafzenderParameters)

                then("there should be no validation errors") {
                    validationResult.isEmpty() shouldBe true
                }
            }
        }

        given("an empty replyTo") {
            val zaakafzenderParameters = createZaakAfzender(
                replyTo = "",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("validating the zaakafzender") {
                val validationResult = validator.validate(zaakafzenderParameters)

                then("there should be no validation errors") {
                    validationResult.isEmpty() shouldBe true
                }
            }
        }
    }
})
