/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import jakarta.ws.rs.BadRequestException
import nl.info.zac.app.shared.RestSorteerRichting

class RestInrichtingscheckParametersTest : BehaviorSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    context("Validating the query parameters of the inrichtingscheck list") {
        given("the default parameters") {
            val restInrichtingscheckParameters = RestInrichtingscheckParameters()

            `when`("they are validated") {
                val violations = validator.validate(restInrichtingscheckParameters)

                then("there are no violations and the first page of 25 is sorted ascending on omschrijving") {
                    violations.shouldBeEmpty()
                    with(restInrichtingscheckParameters) {
                        page shouldBe 0
                        rows shouldBe 25
                        status shouldBe null
                        sort shouldBe RestInrichtingscheckSorteerVeld.OMSCHRIJVING
                        order shouldBe RestSorteerRichting.ASC
                    }
                }
            }
        }

        given("a page size of 100") {
            `when`("the parameters are validated") {
                val violations = validator.validate(RestInrichtingscheckParameters(rows = 100))

                then("there are no violations") {
                    violations.shouldBeEmpty()
                }
            }
        }

        mapOf(
            "a page size of 101" to RestInrichtingscheckParameters(rows = 101),
            "a page size of 0" to RestInrichtingscheckParameters(rows = 0),
            "a negative page" to RestInrichtingscheckParameters(page = -1)
        ).forEach { (description, restInrichtingscheckParameters) ->
            given(description) {
                `when`("the parameters are validated") {
                    val violations = validator.validate(restInrichtingscheckParameters)

                    then("there is one violation, which JAX-RS answers with 400 Bad Request") {
                        violations.size shouldBe 1
                    }
                }
            }
        }
    }

    context("Converting the query parameter values to enums") {
        given("the query values of the sort fields and directions") {
            `when`("they are converted") {
                val sorteerVelden = listOf("omschrijving", "doel", "beginGeldigheid", "valide")
                    .map(RestInrichtingscheckSorteerVeld::fromString)
                val sorteerRichtingen = listOf("asc", "desc").map(RestSorteerRichting::fromString)

                then("each value gives its enum") {
                    sorteerVelden shouldBe RestInrichtingscheckSorteerVeld.entries
                    sorteerRichtingen shouldBe RestSorteerRichting.entries
                }
            }
        }

        given("an unknown sort field") {
            `when`("it is converted") {
                val badRequestException = shouldThrow<BadRequestException> {
                    RestInrichtingscheckSorteerVeld.fromString("identificatie")
                }

                then("the conversion fails with 400 Bad Request") {
                    badRequestException.message shouldBe "Unknown sort field 'identificatie'"
                }
            }
        }

        given("the query values of the statuses") {
            `when`("they are converted") {
                val statuses = listOf("AANDACHT_NODIG", "IN_ORDE").map(RestInrichtingscheckStatus::fromString)

                then("each value gives its enum") {
                    statuses shouldBe RestInrichtingscheckStatus.entries
                }
            }
        }

        given("an unknown status") {
            `when`("it is converted") {
                val badRequestException = shouldThrow<BadRequestException> {
                    RestInrichtingscheckStatus.fromString("ONBEKEND")
                }

                then("the conversion fails with 400 Bad Request") {
                    badRequestException.message shouldBe "Unknown status 'ONBEKEND'"
                }
            }
        }

        given("an unknown sort order") {
            `when`("it is converted") {
                val badRequestException = shouldThrow<BadRequestException> {
                    RestSorteerRichting.fromString("up")
                }

                then("the conversion fails with 400 Bad Request") {
                    badRequestException.message shouldBe "Unknown sort order 'up'"
                }
            }
        }
    }
})
