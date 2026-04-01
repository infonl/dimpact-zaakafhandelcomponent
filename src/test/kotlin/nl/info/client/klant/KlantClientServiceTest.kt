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
import nl.info.client.klant.model.ProductaanvraagSpecificContactDetails
import nl.info.client.klanten.model.generated.CodeObjecttypeEnum
import nl.info.client.klanten.model.generated.CodeRegisterEnum
import nl.info.client.klanten.model.generated.CodeSoortObjectIdEnum
import nl.info.client.klanten.model.generated.Onderwerpobject
import nl.info.client.klanten.model.generated.SoortDigitaalAdresEnum
import nl.info.zac.app.klant.model.contactdetails.ContactDetails
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

    Context("Finding productaanvraag-specific contact details") {
        Given("No klantcontact exists for the given kenmerk") {
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

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        Given("A klantcontact exists for the given kenmerk but has no betrokkenen") {
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

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return null") {
                    result.shouldBeNull()
                }
            }
        }

        Given("A klantcontact with a betrokkene that has both an email and phone digital address") {
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
                klantClient.digitaalAdresList(
                    page = 1,
                    pageSize = 100,
                    verstrektDoorBetrokkeneUuid = betrokkeneUuid.toString()
                )
            } returns createPaginatedDigitaalAdresList(
                listOf(
                    createDigitalAddress(address = emailAddress, soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL),
                    createDigitalAddress(address = telephoneNumber, soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER)
                )
            )

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return the contact details with email and phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                    result?.contactDetails?.telephoneNumber shouldBe telephoneNumber
                }
            }
        }

        Given("A klantcontact with a betrokkene that has only an email digital address") {
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
                klantClient.digitaalAdresList(
                    page = 1,
                    pageSize = 100,
                    verstrektDoorBetrokkeneUuid = betrokkeneUuid.toString()
                )
            } returns createPaginatedDigitaalAdresList(
                listOf(createDigitalAddress(address = emailAddress, soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL))
            )

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return the contact details with email and no phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe emailAddress
                    result?.contactDetails?.telephoneNumber.shouldBeNull()
                }
            }
        }

        Given("A klantcontact with a betrokkene that has only a phone digital address") {
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
                klantClient.digitaalAdresList(
                    page = 1,
                    pageSize = 100,
                    verstrektDoorBetrokkeneUuid = betrokkeneUuid.toString()
                )
            } returns createPaginatedDigitaalAdresList(
                listOf(createDigitalAddress(address = telephoneNumber, soortDigitaalAdres = SoortDigitaalAdresEnum.TELEFOONNUMMER))
            )

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return the contact details with phone and no email") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress.shouldBeNull()
                    result?.contactDetails?.telephoneNumber shouldBe telephoneNumber
                }
            }
        }

        Given("A klantcontact with a betrokkene that has multiple digital addresses of the same type") {
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
                klantClient.digitaalAdresList(
                    page = 1,
                    pageSize = 100,
                    verstrektDoorBetrokkeneUuid = betrokkeneUuid.toString()
                )
            } returns createPaginatedDigitaalAdresList(
                listOf(
                    createDigitalAddress(address = firstEmail, soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL),
                    createDigitalAddress(address = secondEmail, soortDigitaalAdres = SoortDigitaalAdresEnum.EMAIL)
                )
            )

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return the contact details with only the first email and no phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress shouldBe firstEmail
                    result?.contactDetails?.telephoneNumber.shouldBeNull()
                }
            }
        }

        Given("A klantcontact with a betrokkene that has no digital addresses") {
            val kenmerk = "fakeKenmerk"
            val klantcontactUuid = UUID.randomUUID()
            val betrokkeneUuid = UUID.randomUUID()
            val betrokkene = createBetrokkeneForeignKey(uuid = betrokkeneUuid)
            val klantcontact = createKlantcontact(uuid = klantcontactUuid, hadBetrokkenen = listOf(betrokkene))
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
                klantClient.digitaalAdresList(
                    page = 1,
                    pageSize = 100,
                    verstrektDoorBetrokkeneUuid = betrokkeneUuid.toString()
                )
            } returns createPaginatedDigitaalAdresList(emptyList())

            When("productaanvraag-specific contact details are requested") {
                val result = klantClientService.findProductaanvraagSpecificContactDetails(kenmerk)

                Then("it should return the contact details with no email and no phone") {
                    result?.klantcontactUuid shouldBe klantcontactUuid
                    result?.contactDetails?.emailAddress.shouldBeNull()
                    result?.contactDetails?.telephoneNumber.shouldBeNull()
                }
            }
        }
    }

    Context("Linking productaanvraag-specific contact details to a zaak") {
        Given("A productaanvraag-specific contact details and a zaak UUID") {
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

            When("the contact details are linked to the zaak") {
                klantClientService.linkProductaanvraagSpecificContactDetailsToZaak(contactDetails, zaakUuid)

                Then("onderwerpobjectCreate is called with the correct klantcontact UUID and zaak identificator") {
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
