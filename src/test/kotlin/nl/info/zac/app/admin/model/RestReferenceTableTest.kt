/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class RestReferenceTableTest : BehaviorSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    Context("Validation of RestReferenceTable") {
        Given("a reference table with a valid nested value") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(createRestReferenceTableValue(name = "fakeValidName"))
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        Given("a reference table with a nested value that has a blank naam") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(createRestReferenceTableValue(name = ""))
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "waarden[0].naam"
                }
            }
        }

        Given("a reference table with a nested value that has a naam of exactly the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(
                    createRestReferenceTableValue(name = "a".repeat(RestReferenceTableValue.NAAM_MAX_LENGTH))
                )
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        Given("a reference table with a code and naam of exactly the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                code = "a".repeat(RestReferenceTable.CODE_MAX_LENGTH),
                naam = "a".repeat(RestReferenceTable.NAAM_MAX_LENGTH)
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        Given("a reference table with a code exceeding the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                code = "a".repeat(RestReferenceTable.CODE_MAX_LENGTH + 1)
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be one constraint violation on code") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "code"
                }
            }
        }

        Given("a reference table with a naam exceeding the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                naam = "a".repeat(RestReferenceTable.NAAM_MAX_LENGTH + 1)
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be one constraint violation on naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "naam"
                }
            }
        }

        Given("a reference table with a nested value that has a naam exceeding the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(
                    createRestReferenceTableValue(name = "a".repeat(RestReferenceTableValue.NAAM_MAX_LENGTH + 1))
                )
            )

            When("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                Then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "waarden[0].naam"
                }
            }
        }
    }

    Context("Validation of RestReferenceTableUpdate") {
        Given("a reference table update with a valid nested value") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                waarden = listOf(createRestReferenceTableValue(name = "fakeValidName"))
            )

            When("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                Then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        Given("a reference table update with a nested value that has a blank naam") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                waarden = listOf(createRestReferenceTableValue(name = ""))
            )

            When("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                Then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "waarden[0].naam"
                }
            }
        }

        Given("a reference table update with a naam exceeding the maximum length") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                naam = "a".repeat(RestReferenceTable.NAAM_MAX_LENGTH + 1)
            )

            When("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                Then("there should be one constraint violation on naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "naam"
                }
            }
        }

        Given("a reference table update with a code exceeding the maximum length") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                code = "a".repeat(RestReferenceTable.CODE_MAX_LENGTH + 1)
            )

            When("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                Then("there should be one constraint violation on code") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "code"
                }
            }
        }

        Given("a reference table update with a nested value that has a naam exceeding the maximum length") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                waarden = listOf(
                    createRestReferenceTableValue(name = "a".repeat(RestReferenceTableValue.NAAM_MAX_LENGTH + 1))
                )
            )

            When("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                Then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "waarden[0].naam"
                }
            }
        }
    }
})
