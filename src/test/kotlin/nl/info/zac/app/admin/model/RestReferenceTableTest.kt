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

    context("Validation of RestReferenceTable") {
        given("a reference table with a valid nested value") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(createRestReferenceTableValue(name = "fakeValidName"))
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        given("a reference table with a nested value that has a blank naam") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(createRestReferenceTableValue(name = ""))
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "values[0].name"
                }
            }
        }

        given("a reference table with a nested value that has a naam of exactly the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(
                    createRestReferenceTableValue(name = "a".repeat(RestReferenceTableValue.REFERENCE_TABLE_VALUE_MAX_LENGTH))
                )
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        given("a reference table with a code and naam of exactly the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                code = "a".repeat(RestReferenceTable.REFERENCE_TABLE_CODE_MAX_LENGTH),
                naam = "a".repeat(RestReferenceTable.REFERENCE_TABLE_NAME_MAX_LENGTH)
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        given("a reference table with a code exceeding the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                code = "a".repeat(RestReferenceTable.REFERENCE_TABLE_CODE_MAX_LENGTH + 1)
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be one constraint violation on code") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "code"
                }
            }
        }

        given("a reference table with a naam exceeding the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                naam = "a".repeat(RestReferenceTable.REFERENCE_TABLE_NAME_MAX_LENGTH + 1)
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be one constraint violation on name") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "name"
                }
            }
        }

        given("a reference table with a nested value that has a naam exceeding the maximum length") {
            val restReferenceTable = createRestReferenceTable(
                waarden = listOf(
                    createRestReferenceTableValue(name = "a".repeat(RestReferenceTableValue.REFERENCE_TABLE_VALUE_MAX_LENGTH + 1))
                )
            )

            `when`("validating the reference table") {
                val violations = validator.validate(restReferenceTable)

                then("there should be one constraint violation on the nested value's name") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "values[0].name"
                }
            }
        }
    }

    context("Validation of RestReferenceTableUpdate") {
        given("a reference table update with a valid nested value") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                waarden = listOf(createRestReferenceTableValue(name = "fakeValidName"))
            )

            `when`("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        given("a reference table update with a nested value that has a blank naam") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                waarden = listOf(createRestReferenceTableValue(name = ""))
            )

            `when`("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "values[0].name"
                }
            }
        }

        given("a reference table update with a naam exceeding the maximum length") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                naam = "a".repeat(RestReferenceTable.REFERENCE_TABLE_NAME_MAX_LENGTH + 1)
            )

            `when`("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                then("there should be one constraint violation on name") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "name"
                }
            }
        }

        given("a reference table update with a code exceeding the maximum length") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                code = "a".repeat(RestReferenceTable.REFERENCE_TABLE_CODE_MAX_LENGTH + 1)
            )

            `when`("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                then("there should be one constraint violation on code") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "code"
                }
            }
        }

        given("a reference table update with a nested value that has a naam exceeding the maximum length") {
            val restReferenceTableUpdate = createRestReferenceTableUpdate(
                waarden = listOf(
                    createRestReferenceTableValue(name = "a".repeat(RestReferenceTableValue.REFERENCE_TABLE_VALUE_MAX_LENGTH + 1))
                )
            )

            `when`("validating the reference table update") {
                val violations = validator.validate(restReferenceTableUpdate)

                then("there should be one constraint violation on the nested value's naam") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "values[0].name"
                }
            }
        }
    }
})
