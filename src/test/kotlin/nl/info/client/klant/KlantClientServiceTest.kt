/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.ws.rs.NotFoundException
import nl.info.client.klant.model.ProductaanvraagSpecificContactDetails
import nl.info.client.klanten.model.generated.CodeObjecttypeEnum
import nl.info.client.klanten.model.generated.CodeRegisterEnum
import nl.info.client.klanten.model.generated.CodeSoortObjectIdEnum
import nl.info.client.klanten.model.generated.Onderwerpobject
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
import java.util.UUID

@Suppress("LargeClass")
class KlantClientServiceTest : BehaviorSpec({
    val klantClient = mockk<KlantClient>()
    val klantClientService = KlantClientService(klantClient)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Finding digital addresses for natural persons") {
        given("A BSN for which digital addresses exist") {
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

            `when`("digital addresses are retrieved for natuurlijk persoon type with the provided BSN") {
                val result = klantClientService.findDigitalAddressesForNaturalPerson(number)

                then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        given("A BSN for which no digital addresses exist") {
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

            `when`("digital addresses are retrieved for natuurlijk persoon type with the provided BSN") {
                val result = klantClientService.findDigitalAddressesForNaturalPerson(number)

                then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    context("Finding digital addresses for non-natural persons") {
        given("A KVK number for which digital addresses exist") {
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

            `when`("digital addresses are retrieved for niet natuurlijk persoon type with the provided KVK number") {
                val result = klantClientService.findDigitalAddressesForNonNaturalPerson(kvkNumber)

                then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        given("A KVK number for which no digital addresses exist") {
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

            `when`("digital addresses are retrieved for niet natuurlijk persoon type with the provided KVK number") {
                val result = klantClientService.findDigitalAddressesForNonNaturalPerson(number)

                then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    context("Finding digital addresses for vestigingen") {
        given(
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
                    // first partij does have the requested vestigingsnummer but is linked
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
                    // first partij has the requested vestigingsnummer and is linked
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

            `when`("digital addresses are retrieved for vestiging type") {
                val result = klantClientService.findDigitalAddressesForVestiging(vestigingsnummer, kvkNummer)

                then("it should return the digital addresses") {
                    result shouldContainExactly digitalAddresses
                }
            }
        }

        given(
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

            `when`("digital addresses are retrieved for vestiging type") {
                val result = klantClientService.findDigitalAddressesForVestiging(vestigingsnummer, kvkNummer)

                then("it should return the digital addresses") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    context("Listing expand betrokkenen") {
        given("A number for which betrokkenen exist") {
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

            `when`("expand betrokkenen are listed") {
                val result = klantClientService.listExpandBetrokkenen(number, 1)

                then("it should return the expanded betrokkenen") {
                    result shouldContainExactly expandBetrokkenen
                }
            }
        }

        given("A number for which no betrokkenen exist") {
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

            `when`("expand betrokkenen are listed") {
                val result = klantClientService.listExpandBetrokkenen(number, 1)

                then("it should return an empty list") {
                    result.shouldBeEmpty()
                }
            }
        }
    }

    context("Finding productaanvraag-specific contact details") {
        given("No klantcontact exists for the given kenmerk") {
            val kenmerk = "fakeKenmerk"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(emptyList())

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact exists for the given kenmerk but has no betrokkenen") {
            val kenmerk = "fakeKenmerk"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(
                listOf(createKlantcontact(hadBetrokkenen = emptyList()))
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given(
            "A klantcontact with a betrokkene that has both an email and phone digital address marked as non-preferred"
        ) {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
            val emailAddress = "test@example.com"
            val telephoneNumber = "0612345678"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        ),
                        createDigitalAddress(
                            address = telephoneNumber,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return the contact details with email and phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                    result?.contactDetails?.telephoneNumber shouldBe telephoneNumber
                }
            }
        }

        given("A klantcontact with a betrokkene that has only a non-preferred email digital address") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return the contact details with email and no phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                    result?.contactDetails?.telephoneNumber.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has only a non-preferred phone digital address") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
            val telephoneNumber = "0612345678"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = telephoneNumber,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return the contact details with phone and no email") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress.shouldBeNull()
                    result?.contactDetails?.telephoneNumber shouldBe telephoneNumber
                }
            }
        }

        given("A klantcontact with a betrokkene that has multiple non-preferred digital addresses of the same type") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
            val firstEmail = "first@example.com"
            val secondEmail = "second@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = firstEmail,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        ),
                        createDigitalAddress(
                            address = secondEmail,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return the contact details with only the first email and no phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe firstEmail
                    result?.contactDetails?.telephoneNumber.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has no digital addresses") {
            val kenmerk = "fakeKenmerk"
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(digitaleAdressen = emptyList())
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null because there are no non-preferred digital addresses") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that does not exist in klantinteracties") {
            val kenmerk = "fakeKenmerk"
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every { klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid) } throws NotFoundException()

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that throws a NotFoundException") {
            val kenmerk = "fakeKenmerk"
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every { klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid) } throws NotFoundException()

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that is linked to a partij but has a non-preferred digital address") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                wasPartij = createPartijForeignKey(),
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return the non-preferred contact details regardless of the partij link") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                }
            }
        }

        given("A klantcontact with a betrokkene whose digital addresses are all preferred (isStandaardAdres = true)") {
            val kenmerk = "fakeKenmerk"
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = "saved@example.com",
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = true
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then(
                    "it should return null because preferred addresses are the citizen's saved preference, not aanvraag-specific"
                ) {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has a mix of preferred and non-preferred digital addresses") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
            val preferredEmail = "preferred@example.com"
            val aanvraagSpecificPhone = "0612345678"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = preferredEmail,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = true
                        ),
                        createDigitalAddress(
                            address = aanvraagSpecificPhone,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return only the non-preferred address, excluding the preferred email") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress.shouldBeNull()
                    result?.contactDetails?.telephoneNumber shouldBe aanvraagSpecificPhone
                }
            }
        }

        given("A klantcontact with a betrokkene that has initiator = false") {
            val kenmerk = "fakeKenmerk"
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(uuid = betrokkeneUuid, initiator = false)

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null because the betrokkene is not the initiator") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has initiator = null") {
            val kenmerk = "fakeKenmerk"
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(uuid = betrokkeneUuid).apply { this.initiator = null }

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return null because the betrokkene has no initiator flag set") {
                    result.shouldBeNull()
                }
            }
        }

        given(
            "A klantcontact with multiple betrokkenen where the first throws NotFoundException and the second is a valid initiator klant"
        ) {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val notFoundUuid = UUID.randomUUID()
            val initiatorUuid = UUID.randomUUID()
            val klantcontact = createKlantcontact(
                uuid = klantcontactUuid,
                hadBetrokkenen = listOf(
                    createBetrokkeneForeignKey(uuid = notFoundUuid),
                    createBetrokkeneForeignKey(uuid = initiatorUuid)
                )
            )
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(notFoundUuid)
            } throws NotFoundException("betrokkene not found")
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(initiatorUuid)
            } returns createExpandBetrokkene(
                uuid = initiatorUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should skip the not-found betrokkene and return contact details from the second") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                }
            }
        }

        given("A klantcontact with multiple betrokkenen where the first is not the initiator klant but the second is") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val nonInitiatorUuid = UUID.randomUUID()
            val initiatorUuid = UUID.randomUUID()
            val klantcontact = createKlantcontact(
                uuid = klantcontactUuid,
                hadBetrokkenen = listOf(
                    createBetrokkeneForeignKey(uuid = nonInitiatorUuid),
                    createBetrokkeneForeignKey(uuid = initiatorUuid)
                )
            )
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "formulierinzending",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "Open Formulieren",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "public_registration_reference",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = kenmerk
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(nonInitiatorUuid)
            } returns createExpandBetrokkene(uuid = nonInitiatorUuid, initiator = false)
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(initiatorUuid)
            } returns createExpandBetrokkene(
                uuid = initiatorUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                then("it should return the contact details from the initiator klant betrokkene") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                }
            }
        }
    }

    context("Finding zaak-specific contact details") {
        given("No klantcontact exists for the given zaak UUID") {
            val zaakUuid = UUID.randomUUID()
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(emptyList())

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact exists for the given zaak UUID but has no betrokkenen") {
            val zaakUuid = UUID.randomUUID()
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(
                listOf(createKlantcontact(hadBetrokkenen = emptyList()))
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given(
            "A klantcontact with a betrokkene that has both an email and phone digital address marked as non-preferred"
        ) {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            val emailAddress = "test@example.com"
            val telephoneNumber = "0612345678"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        ),
                        createDigitalAddress(
                            address = telephoneNumber,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return the contact details with email and phone") {
                    result?.emailAddress shouldBe emailAddress
                    result?.telephoneNumber shouldBe telephoneNumber
                }
            }
        }

        given("A klantcontact with a betrokkene that has only a non-preferred email digital address") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return the contact details with email and no phone") {
                    result?.emailAddress shouldBe emailAddress
                    result?.telephoneNumber.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has only a non-preferred phone digital address") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            val telephoneNumber = "0612345678"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = telephoneNumber,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return the contact details with phone and no email") {
                    result?.emailAddress.shouldBeNull()
                    result?.telephoneNumber shouldBe telephoneNumber
                }
            }
        }

        given("A klantcontact with a betrokkene that has no digital addresses") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(digitaleAdressen = emptyList())
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null because there are no non-preferred digital addresses") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that does not exist in klantinteracties") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every { klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid) } throws NotFoundException()

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that throws a NotFoundException") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every { klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid) } throws NotFoundException()

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that is linked to a partij but has a non-preferred digital address") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                wasPartij = createPartijForeignKey(),
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return the non-preferred contact details regardless of the partij link") {
                    result?.emailAddress shouldBe emailAddress
                }
            }
        }

        given("A klantcontact with a betrokkene whose digital addresses are all preferred (isStandaardAdres = true)") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = "saved@example.com",
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = true
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then(
                    "it should return null because preferred addresses are the citizen's saved preference, not zaak-specific"
                ) {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has a mix of preferred and non-preferred digital addresses") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            val preferredEmail = "preferred@example.com"
            val zaakSpecificPhone = "0612345678"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(
                uuid = betrokkeneUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = preferredEmail,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = true
                        ),
                        createDigitalAddress(
                            address = zaakSpecificPhone,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return only the non-preferred address, excluding the preferred email") {
                    result?.emailAddress.shouldBeNull()
                    result?.telephoneNumber shouldBe zaakSpecificPhone
                }
            }
        }

        given("A klantcontact with a betrokkene that has initiator = false") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(uuid = betrokkeneUuid, initiator = false)

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null because the betrokkene is not the initiator") {
                    result.shouldBeNull()
                }
            }
        }

        given("A klantcontact with a betrokkene that has initiator = null") {
            val zaakUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(hadBetrokkenen = listOf(betrokkene))
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(betrokkeneUuid)
            } returns createExpandBetrokkene(uuid = betrokkeneUuid).apply { this.initiator = null }

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return null because the betrokkene has no initiator flag set") {
                    result.shouldBeNull()
                }
            }
        }

        given(
            "A klantcontact with multiple betrokkenen where the first throws NotFoundException and the second is a valid initiator klant"
        ) {
            val zaakUuid = UUID.randomUUID()
            val notFoundUuid = UUID.randomUUID()
            val initiatorUuid = UUID.randomUUID()
            val klantcontact = createKlantcontact(
                hadBetrokkenen = listOf(
                    createBetrokkeneForeignKey(uuid = notFoundUuid),
                    createBetrokkeneForeignKey(uuid = initiatorUuid)
                )
            )
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(notFoundUuid)
            } throws NotFoundException("betrokkene not found")
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(initiatorUuid)
            } returns createExpandBetrokkene(
                uuid = initiatorUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should skip the not-found betrokkene and return contact details from the second") {
                    result?.emailAddress shouldBe emailAddress
                }
            }
        }

        given("A klantcontact with multiple betrokkenen where the first is not the initiator klant but the second is") {
            val zaakUuid = UUID.randomUUID()
            val nonInitiatorUuid = UUID.randomUUID()
            val initiatorUuid = UUID.randomUUID()
            val klantcontact = createKlantcontact(
                hadBetrokkenen = listOf(
                    createBetrokkeneForeignKey(uuid = nonInitiatorUuid),
                    createBetrokkeneForeignKey(uuid = initiatorUuid)
                )
            )
            val emailAddress = "test@example.com"
            every {
                klantClient.klantcontactList(
                    page = 1,
                    pageSize = 100,
                    onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype = "zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeRegister = "open-zaak",
                    onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId = "uuid",
                    onderwerpobjectOnderwerpobjectidentificatorObjectId = zaakUuid.toString()
                )
            } returns createPaginatedKlantcontactList(listOf(klantcontact))
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(nonInitiatorUuid)
            } returns createExpandBetrokkene(uuid = nonInitiatorUuid, initiator = false)
            every {
                klantClient.getBetrokkeneWithDigitaleAdressen(initiatorUuid)
            } returns createExpandBetrokkene(
                uuid = initiatorUuid,
                expand = createExpandBetrokkeneAllOfExpand(
                    digitaleAdressen = listOf(
                        createDigitalAddress(
                            address = emailAddress,
                            soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL,
                            isStandaardAdres = false
                        )
                    )
                )
            )

            `when`("zaak-specific contact details are requested") {
                val result = klantClientService.findZaakSpecificContactDetails(zaakUuid)

                then("it should return the contact details from the initiator klant betrokkene") {
                    result?.emailAddress shouldBe emailAddress
                }
            }
        }
    }

    context("Linking productaanvraag-specific contact details to a zaak") {
        given("A productaanvraag-specific contact details and a zaak UUID") {
            val klantcontactUuid = UUID.randomUUID()
            val zaakUuid = UUID.randomUUID()
            val contactDetails = ProductaanvraagSpecificContactDetails(
                klantcontactUuid = klantcontactUuid,
                contactDetails = ContactDetails(
                    emailAddress = "test@example.com",
                    telephoneNumber = "0612345678"
                )
            )
            val onderwerpobjectSlot = slot<Onderwerpobject>()
            every { klantClient.onderwerpobjectCreate(capture(onderwerpobjectSlot)) } returns Onderwerpobject()

            `when`("the contact details are linked to the zaak") {
                klantClientService.linkProductaanvraagSpecificContactDetailsToZaak(contactDetails, zaakUuid)

                then("onderwerpobjectCreate is called with the correct klantcontact UUID and zaak identificator") {
                    val captured = onderwerpobjectSlot.captured
                    captured.klantcontact.uuid shouldBe klantcontactUuid
                    captured.onderwerpobjectidentificator.objectId shouldBe zaakUuid.toString()
                    captured.onderwerpobjectidentificator.codeObjecttype shouldBe "zaak"
                    captured.onderwerpobjectidentificator.codeRegister shouldBe "open-zaak"
                    captured.onderwerpobjectidentificator.codeSoortObjectId shouldBe "uuid"
                }
            }
        }
    }
})
