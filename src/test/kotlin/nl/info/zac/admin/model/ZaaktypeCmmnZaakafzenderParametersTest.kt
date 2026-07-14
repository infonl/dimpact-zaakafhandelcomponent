/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.validation.Validation

class ZaaktypeCmmnZaakafzenderParametersTest : BehaviorSpec({
    val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

    context("equals") {
        given("Two equal objects") {
            val zaakafzenderParameters1 = createZaakAfzender(zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration)
            val zaakafzenderParameters2 = createZaakAfzender(zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration)

            `when`("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be equal") {
                    comparisonResult shouldBe true
                }

                And("they should have the same hashcode") {
                    hashCode1 shouldBe hashCode2
                }
            }
        }

        given("Two objects differ in the defaultMail property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                defaultMail = true,
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                defaultMail = false,
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }

        given("Two objects differ in the mail property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                mail = "mail1@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                mail = "mail2@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }

        given("Two objects differ in the replyTo property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                replyTo = "mail1@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                replyTo = "mail2@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }

        given("Two objects differ in multiple properties") {
            val zaakafzenderParameters1 = createZaakAfzender(
                defaultMail = true,
                replyTo = "mail1@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                defaultMail = false,
                replyTo = "mail2@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }
    }

    context("validation") {
        val validator = Validation.buildDefaultValidatorFactory().validator

        given("a valid zaakafzender") {
            val zaakafzenderParameters = createZaakAfzender(zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration)

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
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
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
