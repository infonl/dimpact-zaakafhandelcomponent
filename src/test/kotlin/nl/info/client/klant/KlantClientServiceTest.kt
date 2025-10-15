/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.klant.model.CodeObjecttypeEnum

class KlantClientServiceTest : BehaviorSpec({
    val klantClient = mockk<KlantClient>()
    val klantClientService = KlantClientService(klantClient)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Finding digital addresses") {
        Given("A number for which digital addresses exist") {
            val number = "12345"
            val digitalAddresses = createDigitalAddresses()
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorCodeObjecttype = "natuurlijk_persoon",
                    partijIdentificatorObjectId = number
                )
            } returns mockk {
                every { getResults() } returns listOf(
                    mockk {
                        every { getExpand()?.getDigitaleAdressen() } returns digitalAddresses
                    }
                )
            }

            When("digital addresses are retrieved for natuurlijk persoon type") {
                val result = klantClientService.findDigitalAddresses(CodeObjecttypeEnum.NATUURLIJK_PERSOON, number)

                Then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        Given("A number for which no digital addresses exist") {
            val number = "67890"
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorCodeObjecttype = "vestiging",
                    partijIdentificatorObjectId = number
                )
            } returns mockk {
                every { getResults() } returns listOf(
                    mockk {
                        every { getExpand()?.getDigitaleAdressen() } returns null
                    }
                )
            }

            When("digital addresses are retrieved for vestiging type") {
                val result = klantClientService.findDigitalAddresses(CodeObjecttypeEnum.VESTIGING, number)

                Then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    Context("Listing betrokkenen") {
        Given("A number for which betrokkenen exist") {
            val number = "12345"
            val expandBetrokkenen = listOf(
                createExpandBetrokkene(fullName = "fakeFullName1"),
                createExpandBetrokkene(fullName = "fakeFullName2")
            )
            every {
                klantClient.partijenList(
                    expand = "betrokkenen,betrokkenen.hadKlantcontact",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorObjectId = number
                )
            } returns mockk {
                every { getResults() } returns listOf(
                    mockk {
                        every { getExpand()?.betrokkenen } returns expandBetrokkenen
                    }
                )
            }

            When("betrokkenen are listed") {
                val result = klantClientService.listBetrokkenen(number, 1)

                Then("it should return the digital addresses") {
                    result shouldContainExactly expandBetrokkenen
                }
            }
        }

        Given("A number for which no betrokkenen exist") {
            val number = "67890"
            every {
                klantClient.partijenList(
                    expand = "betrokkenen,betrokkenen.hadKlantcontact",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorObjectId = number
                )
            } returns mockk {
                every { getResults() } returns listOf(
                    mockk {
                        every { getExpand()?.betrokkenen } returns null
                    }
                )
            }

            When("betrokkenen are listed") {
                val result = klantClientService.listBetrokkenen(number, 1)

                Then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }
})
