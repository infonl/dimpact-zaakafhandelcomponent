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
import nl.info.client.klanten.model.generated.CodeObjecttypeEnum
import nl.info.client.klanten.model.generated.CodeRegisterEnum
import nl.info.client.klanten.model.generated.CodeSoortObjectIdEnum
import java.util.UUID

class KlantClientServiceTest : BehaviorSpec({
    val klantClient = mockk<KlantClient>()
    val klantClientService = KlantClientService(klantClient)

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("Finding digital addresses for natural persons") {
        Given("A BSN for which digital addresses exist") {
            val number = "12345"
            val digitalAddresses = createDigitalAddresses()
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 100,
                    partijIdentificatorCodeObjecttype = "natuurlijk_persoon",
                    partijIdentificatorCodeSoortObjectId = "bsn",
                    partijIdentificatorObjectId = number
                )
            } returns createPaginatedExpandPartijList(
                listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = digitalAddresses
                        )
                    )
                )
            )

            When("digital addresses are retrieved for natuurlijk persoon type with the provided BSN") {
                val result = klantClientService.findDigitalAddressesForNaturalPerson(number)

                Then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        Given("A BSN for which no digital addresses exist") {
            val number = "12345"
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 100,
                    partijIdentificatorCodeObjecttype = "natuurlijk_persoon",
                    partijIdentificatorCodeSoortObjectId = "bsn",
                    partijIdentificatorObjectId = number
                )
            } returns createPaginatedExpandPartijList(
                listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = null
                        )
                    )
                )
            )

            When("digital addresses are retrieved for natuurlijk persoon type with the provided BSN") {
                val result = klantClientService.findDigitalAddressesForNaturalPerson(number)

                Then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    Context("Finding digital addresses for non-natural persons") {
        Given("A KVK number for which digital addresses exist") {
            val kvkNumber = "54321"
            val digitalAddresses = createDigitalAddresses()
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 100,
                    partijIdentificatorCodeObjecttype = "niet_natuurlijk_persoon",
                    partijIdentificatorCodeSoortObjectId = "kvk_nummer",
                    partijIdentificatorObjectId = kvkNumber
                )
            } returns createPaginatedExpandPartijList(
                listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = digitalAddresses
                        )
                    )
                )
            )

            When("digital addresses are retrieved for niet natuurlijk persoon type with the provided KVK number") {
                val result = klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNumber)

                Then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        Given("A KVK number for which no digital addresses exist") {
            val number = "54321"
            every {
                klantClient.partijenList(
                    expand = "digitaleAdressen",
                    page = 1,
                    pageSize = 100,
                    partijIdentificatorCodeObjecttype = "niet_natuurlijk_persoon",
                    partijIdentificatorCodeSoortObjectId = "kvk_nummer",
                    partijIdentificatorObjectId = number
                )
            } returns createPaginatedExpandPartijList(
                listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            digitaleAdressen = null
                        )
                    )
                )
            )

            When("digital addresses are retrieved for niet natuurlijk persoon type with the provided KVK number") {
                val result = klantClientService.findDigitalAddressesForNonNaturalPerson(number)

                Then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    Context("Finding digital addresses for vestigingen") {
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
                    codeObjecttype = CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON,
                    codeRegister = CodeRegisterEnum.HR,
                    codeSoortObjectId = CodeSoortObjectIdEnum.KVK_NUMMER,
                    objectId = kvkNummer
                )
            )
            val otherKvkIdentificator = createPartijIdentificator(
                partijIdentificator = createPartijIdentificatorGroepType(
                    codeObjecttype = CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON,
                    codeRegister = CodeRegisterEnum.HR,
                    codeSoortObjectId = CodeSoortObjectIdEnum.KVK_NUMMER,
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
                                    codeObjecttype = CodeObjecttypeEnum.VESTIGING,
                                    codeRegister = CodeRegisterEnum.HR,
                                    codeSoortObjectId = CodeSoortObjectIdEnum.VESTIGINGSNUMMER,
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
                                    codeObjecttype = CodeObjecttypeEnum.VESTIGING,
                                    codeRegister = CodeRegisterEnum.HR,
                                    codeSoortObjectId = CodeSoortObjectIdEnum.VESTIGINGSNUMMER,
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
                    pageSize = 100,
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
                    codeObjecttype = CodeObjecttypeEnum.NIET_NATUURLIJK_PERSOON,
                    codeRegister = CodeRegisterEnum.HR,
                    codeSoortObjectId = CodeSoortObjectIdEnum.KVK_NUMMER,
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
                                    codeObjecttype = CodeObjecttypeEnum.VESTIGING,
                                    codeRegister = CodeRegisterEnum.HR,
                                    codeSoortObjectId = CodeSoortObjectIdEnum.VESTIGINGSNUMMER,
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
                    pageSize = 100,
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

    Context("Listing expand betrokkenen") {
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
                    pageSize = 100,
                    partijIdentificatorObjectId = number
                )
            } returns createPaginatedExpandPartijList(
                listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            betrokkenen = expandBetrokkenen
                        )
                    )
                )
            )

            When("expand betrokkenen are listed") {
                val result = klantClientService.listExpandBetrokkenen(number, 1)

                Then("it should return the expanded betrokkenen") {
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
                    pageSize = 100,
                    partijIdentificatorObjectId = number
                )
            } returns createPaginatedExpandPartijList(
                listOf(
                    createExpandPartij(
                        expand = createExpandPartijAllOfExpand(
                            betrokkenen = null
                        )
                    )
                )
            )

            When("expand betrokkenen are listed") {
                val result = klantClientService.listExpandBetrokkenen(number, 1)

                Then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }
})
