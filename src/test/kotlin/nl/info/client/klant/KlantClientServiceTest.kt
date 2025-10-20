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
import java.util.UUID

class KlantClientServiceTest : BehaviorSpec({
    val klantClient = mockk<KlantClient>()
    val klantClientService = KlantClientService(klantClient)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Finding digital addresses") {
        Given("A BSN for which digital addresses exist") {
            val number = "12345"
            val digitalAddresses = createDigitalAddresses()
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorCodeObjecttype = "natuurlijk_persoon",
                    partijIdentificatorCodeSoortObjectId = "bsn",
                    partijIdentificatorObjectId = number
                )
            } returns mockk {
                every { getResults() } returns listOf(
                    mockk {
                        every { getExpand()?.getDigitaleAdressen() } returns digitalAddresses
                    }
                )
            }

            When("digital addresses are retrieved for natuurlijk persoon type with the provided BSN") {
                val result = klantClientService.findDigitalAddressesForNaturalPerson(number)

                Then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        Given(
            """
                A vestigingsnummer and KVK nummer combination for which digital addresses exist,
                and where there are multiple partijen for the vestigingsnummer, only one of which
                is linked to the requested KVK number
                """
        ) {
            val kvkNummer = "54321"
            val otherKvkNummer = "99999"
            val vestigingsnummer = "67890"
            val digitalAddresses = createDigitalAddresses()
            val kvkIdentificatorUUID = UUID.randomUUID()
            val otherKvkIdentificatorUUID = UUID.randomUUID()
            val kvkIdentificator = createPartijIdentificator(
                partijIdentificator = createPartijIdentificatorGroepType(
                    codeObjecttype = "niet_natuurlijk_persoon",
                    codeRegister = "fakeCodeRegister",
                    codeSoortObjectId = "kvk_nummer",
                    objectId = kvkNummer
                )
            )
            val otherKvkIdentificator = createPartijIdentificator(
                partijIdentificator = createPartijIdentificatorGroepType(
                    codeObjecttype = "niet_natuurlijk_persoon",
                    codeRegister = "fakeCodeRegister",
                    codeSoortObjectId = "kvk_nummer",
                    objectId = otherKvkNummer
                )
            )
            val paginatedExpandPartijList = createPaginatedExpandPartijList(
                expandPartijen = listOf(
                    // first partij does have the requested vestingsnummer but is linked
                    // to a partij with a different KVK number and also has different digital addresses
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = createDigitalAddresses(
                                phone = "020-0000000",
                                email = "fake-email@example.com"
                            )
                        ),
                        partijIdentificatoren = listOf(
                            createPartijIdentificator(
                                partijIdentificator = createPartijIdentificatorGroepType(
                                    codeObjecttype = "vestiging",
                                    codeRegister = "fakeCodeRegister",
                                    codeSoortObjectId = "vestigingsnummer",
                                    objectId = vestigingsnummer
                                ),
                                subIdentificatorVan = createPartijIdentificatorForeignkey(
                                    uuid = otherKvkIdentificatorUUID
                                )
                            )
                        )
                    ),
                    // first partij has the requested vestingsnummer and is linked
                    // to a partij with the requested KVK number
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = digitalAddresses
                        ),
                        partijIdentificatoren = listOf(
                            createPartijIdentificator(
                                partijIdentificator = createPartijIdentificatorGroepType(
                                    codeObjecttype = "vestiging",
                                    codeRegister = "fakeCodeRegister",
                                    codeSoortObjectId = "vestigingsnummer",
                                    objectId = vestigingsnummer
                                ),
                                subIdentificatorVan = createPartijIdentificatorForeignkey(
                                    uuid = kvkIdentificatorUUID
                                )
                            )
                        )
                    )
                )
            )
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorCodeObjecttype = "vestiging",
                    partijIdentificatorCodeSoortObjectId = "vestigingsnummer",
                    partijIdentificatorObjectId = vestigingsnummer
                )
            } returns paginatedExpandPartijList
            every { klantClient.getPartijIdentificator(kvkIdentificatorUUID) } returns kvkIdentificator
            every { klantClient.getPartijIdentificator(otherKvkIdentificatorUUID) } returns otherKvkIdentificator

            When("digital addresses are retrieved for vestiging type") {
                val result = klantClientService.findDigitalAddressesForVestiging(vestigingsnummer, kvkNummer)

                Then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        Given(
            """
                A vestigingsnummer and a KVK number but where the vestiging partij has a link to a related 
                'sub-identificator-van' partij which has a different KVK number
                """
        ) {
            val kvkNummer = "54321"
            val subIdentificatorVanKvkNummer = "99999"
            val vestigingsnummer = "67890"
            val digitalAddresses = createDigitalAddresses()
            val kvkIdentificatorUUID = UUID.randomUUID()
            val kvkIdentificator = createPartijIdentificator(
                partijIdentificator = createPartijIdentificatorGroepType(
                    codeObjecttype = "niet_natuurlijk_persoon",
                    codeRegister = "fakeCodeRegister",
                    codeSoortObjectId = "kvk_nummer",
                    objectId = subIdentificatorVanKvkNummer
                )
            )
            val paginatedExpandPartijList = createPaginatedExpandPartijList(
                expandPartijen = listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = digitalAddresses
                        ),
                        partijIdentificatoren = listOf(
                            createPartijIdentificator(
                                partijIdentificator = createPartijIdentificatorGroepType(
                                    codeObjecttype = "vestiging",
                                    codeRegister = "fakeCodeRegister",
                                    codeSoortObjectId = "vestigingsnummer",
                                    objectId = vestigingsnummer
                                ),
                                subIdentificatorVan = createPartijIdentificatorForeignkey(
                                    uuid = kvkIdentificatorUUID
                                )
                            )
                        )
                    )
                )
            )
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 1,
                    partijIdentificatorCodeObjecttype = "vestiging",
                    partijIdentificatorCodeSoortObjectId = "vestigingsnummer",
                    partijIdentificatorObjectId = vestigingsnummer
                )
            } returns paginatedExpandPartijList
            every { klantClient.getPartijIdentificator(kvkIdentificatorUUID) } returns kvkIdentificator

            When("digital addresses are retrieved for vestiging type") {
                val result = klantClientService.findDigitalAddressesForVestiging(vestigingsnummer, kvkNummer)

                Then("it should return the digital addresses") {
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
