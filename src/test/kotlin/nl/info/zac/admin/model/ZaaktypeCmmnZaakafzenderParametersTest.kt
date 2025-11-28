/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ZaaktypeCmmnZaakafzenderParametersTest : BehaviorSpec({
    val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

    Context("equals") {
        Given("Two equal objects") {
            val zaakafzenderParameters1 = createZaakAfzender(zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration)
            val zaakafzenderParameters2 = createZaakAfzender(zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration)

            When("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                Then("they should be equal") {
                    comparisonResult shouldBe true
                }

                And("they should have the same hashcode") {
                    hashCode1 shouldBe hashCode2
                }
            }
        }

        Given("Two objects differ in the defaultMail property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                defaultMail = true,
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                defaultMail = false,
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            When("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                Then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }

        Given("Two objects differ in the mail property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                mail = "mail1@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                mail = "mail2@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            When("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                Then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }

        Given("Two objects differ in the replyTo property") {
            val zaakafzenderParameters1 = createZaakAfzender(
                replyTo = "mail1@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters2 = createZaakAfzender(
                replyTo = "mail2@example.com",
                zaaktypeCmmnConfiguration = zaaktypeCmmnConfiguration
            )

            When("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                Then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }

        Given("Two objects differ in multiple properties") {
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

            When("they are compared") {
                val comparisonResult = zaakafzenderParameters1.equals(zaakafzenderParameters2)
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                Then("they should be different") {
                    comparisonResult shouldBe false
                }

                And("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }
    }
})
