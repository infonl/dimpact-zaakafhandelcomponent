/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.validation.Validation

class ZaaktypeZaakafzenderParametersTest : BehaviorSpec({
    val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()

    context("equals") {
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
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    isEqual shouldBe false
                }

                and("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
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
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    isEqual shouldBe false
                }

                and("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
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
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    isEqual shouldBe false
                }

                and("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
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
                val hashCode1 = zaakafzenderParameters1.hashCode()
                val hashCode2 = zaakafzenderParameters2.hashCode()

                then("they should be different") {
                    isEqual shouldBe false
                }

                and("they should have different hashcodes") {
                    hashCode1 shouldNotBe hashCode2
                }
            }
        }
    }

    context("isModifiedFrom") {
        given("an original zaakafzender and a zaakafzender with the same mail, defaultMail and replyTo") {
            val original = createZaakAfzender(zaaktypeConfiguration = zaaktypeCmmnConfiguration)
            val zaakafzenderParameters = createZaakAfzender(zaaktypeConfiguration = zaaktypeCmmnConfiguration)

            `when`("checking whether it is modified from the original") {
                val isModified = zaakafzenderParameters.isModifiedFrom(original)

                then("it is not modified") {
                    isModified shouldBe false
                }
            }
        }

        given("an original zaakafzender and a zaakafzender with the same mail but a different defaultMail") {
            val original = createZaakAfzender(defaultMail = false, zaaktypeConfiguration = zaaktypeCmmnConfiguration)
            val zaakafzenderParameters = createZaakAfzender(
                defaultMail = true,
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("checking whether it is modified from the original") {
                val isModified = zaakafzenderParameters.isModifiedFrom(original)

                then("it is modified") {
                    isModified shouldBe true
                }
            }
        }

        given("an original zaakafzender and a zaakafzender with the same mail but a different replyTo") {
            val original = createZaakAfzender(
                replyTo = "fakeReplyTo1@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters = createZaakAfzender(
                replyTo = "fakeReplyTo2@example.com",
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("checking whether it is modified from the original") {
                val isModified = zaakafzenderParameters.isModifiedFrom(original)

                then("it is modified") {
                    isModified shouldBe true
                }
            }
        }

        given("an original zaakafzender and a zaakafzender with a different mail and a different defaultMail") {
            val original = createZaakAfzender(
                mail = "fakeMail1@example.com",
                defaultMail = false,
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )
            val zaakafzenderParameters = createZaakAfzender(
                mail = "fakeMail2@example.com",
                defaultMail = true,
                zaaktypeConfiguration = zaaktypeCmmnConfiguration
            )

            `when`("checking whether it is modified from the original") {
                val isModified = zaakafzenderParameters.isModifiedFrom(original)

                then("it is not a modification of the original, because the mail identifies another zaakafzender") {
                    isModified shouldBe false
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
