/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.NotFoundException
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.model.generated.PersonenQuery
import nl.info.client.brp.util.BrpProtocolleringContext
import nl.info.client.brp.util.createBrpConfiguration
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import java.util.Optional
import java.util.UUID

class BrpClientServiceTest : BehaviorSpec({
    val doelbindingZoekMetDefault = "fakeDoelbindingZoekMetDefault"
    val doelbindingRaadpleegMetDefault = "fakeDoelbindingRaadpleegMetDefault"
    val verwerkingregisterDefault = "fakeVerwerkingregisterDefault"
    val zaaktypeUuid = UUID.randomUUID()
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val zaaktypeConfigurationService: ZaaktypeConfigurationService = mockk()
    val brpConfiguration = createBrpConfiguration(
        doelbindingZoekMetDefault = Optional.of(doelbindingZoekMetDefault),
        doelbindingRaadpleegMetDefault = Optional.of(doelbindingRaadpleegMetDefault),
        verwerkingregisterDefault = Optional.of(verwerkingregisterDefault)
    )
    val configuredBrpClientService = BrpClientService(
        personenApi = personenApi,
        brpConfiguration = brpConfiguration,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        brpProtocolleringContext = BrpProtocolleringContext()
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A person for a given BSN") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "Leerplicht"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("it should return the person and set doelbinding and verwerking in context headers") {
                personResponse shouldBe person
                localContext.headers["x-doelbinding"] shouldBe retrievePersoonPurpose
                localContext.headers["x-verwerking"] shouldBe "$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"
            }
        }
    }

    given("A person for a given BSN and a zaaktype without a configuration") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        every { zaaktypeConfigurationService.findConfiguration(zaaktypeUuid) } returns null
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("it should return the person and use the default doelbinding and verwerking") {
                personResponse shouldBe person
                localContext.headers["x-doelbinding"] shouldBe doelbindingRaadpleegMetDefault
                localContext.headers["x-verwerking"] shouldBe verwerkingregisterDefault
            }
        }
    }

    given("No person for a given BSN") {
        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = emptyList()
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", zaaktypeUuid, "fakeTestUser")

            then("it should return null") {
                personResponse shouldBe null
            }
        }
    }

    given("Multiple persons for a given BSN") {
        val persons = listOf(
            createPersoon(bsn = "123456789"),
            createPersoon(bsn = "123456789")
        )
        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = persons
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", zaaktypeUuid, "fakeTestUser")

            then("it should return the first person") {
                personResponse shouldBe persons[0]
            }
        }
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        given("Another person for a given BSN and a $configurationType zaaktype configuration with BRP doelbindingen") {
            val bsn = "123456789"
            val person = createPersoon(bsn = bsn)
            val queryPersonenPurpose = "zoekWaarde"
            val zaaktypeConfiguration = createZaaktypeConfiguration(UUID.randomUUID()).apply {
                zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                    zoekWaarde = queryPersonenPurpose
                    verwerkingregisterWaarde = "Leerplicht"
                }
            }
            val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
                persons = listOf(person)
            )

            every {
                zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
            } returns zaaktypeConfiguration
            every { personenApi.personen(any<PersonenQuery>()) } returns raadpleegMetBurgerservicenummerResponse
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = brpConfiguration,
                zaaktypeConfigurationService = zaaktypeConfigurationService,
                brpProtocolleringContext = localContext
            )

            `when`("a query is run on personen for this BSN") {
                val personResponse = localService.queryPersonen(
                    createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                    zaaktypeUuid,
                    "fakeTestUser"
                )

                then("it should return the person and set doelbinding in context headers") {
                    personResponse shouldBe raadpleegMetBurgerservicenummerResponse
                    localContext.headers["x-doelbinding"] shouldBe queryPersonenPurpose
                    localContext.headers["x-verwerking"] shouldBe "Leerplicht@${zaaktypeConfiguration.zaaktypeOmschrijving}"
                }
            }
        }
    }

    given("Unicode processing value configured for BRP person retrieval") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "Bíj́na"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = createBrpConfiguration(),
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("it should still return the person with default verwerking value used for unicode processing") {
                personResponse shouldBe person
                localContext.headers["x-verwerking"] shouldBe "$verwerkingregisterDefault@fakeZaaktypeOmschrijving"
            }
        }
    }

    given("Processing value with whitespaces") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "  \t Process ing\tvalue\t with whitespaces \t"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = createBrpConfiguration(),
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("it should still return the person with trimmed processing value") {
                personResponse shouldBe person
                localContext.headers["x-verwerking"] shouldBe "Process ing\tvalue\t with whitespaces@fakeZaaktypeOmschrijving"
            }
        }
    }

    given("Only whitespaces for BRP in zaakafhandelparameters") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                zoekWaarde = ""
                raadpleegWaarde = ""
                verwerkingregisterWaarde = ""
            }
        )

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = createBrpConfiguration(),
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("it should still return the person using defaults from context headers") {
                personResponse shouldBe person
                localContext.headers["x-doelbinding"] shouldBe doelbindingRaadpleegMetDefault
                localContext.headers["x-verwerking"] shouldBe "$verwerkingregisterDefault@fakeZaaktypeOmschrijving"
            }
        }
    }

    given("A person exists for a given BSN, but no zaaktype is found for the given audit event") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("retrieve persoon is called") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("retrieving a person should still work with default doelbinding and verwerking in context headers") {
                personResponse shouldBe person
                localContext.headers["x-doelbinding"] shouldBe doelbindingRaadpleegMetDefault
                localContext.headers["x-verwerking"] shouldBe verwerkingregisterDefault
            }
        }
    }

    given("A person exists for a given BSN and the zaaktype has no zaaktype configuration") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        every { zaaktypeConfigurationService.findConfiguration(zaaktypeUuid) } returns null
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("retrieve persoon is called") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("the person is returned with the default doelbinding and verwerking in the context headers") {
                personResponse shouldBe person
                localContext.headers["x-doelbinding"] shouldBe doelbindingRaadpleegMetDefault
                localContext.headers["x-verwerking"] shouldBe verwerkingregisterDefault
            }
        }
    }

    given("A logged-in user is provided") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "Leerplicht"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )
        val userName = "fakeUserName"

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, userName)

            then("it should return the person and set gebruiker in context headers") {
                personResponse shouldBe person
                localContext.headers["x-gebruiker"] shouldBe userName
            }
        }
    }

    given("An API key is configured") {
        val bsn = "123456789"
        val apiKey = "fake-api-key"
        val configWithApiKey = createBrpConfiguration(
            apiKey = Optional.of(apiKey),
            headerNameApiKey = Optional.of("x-api-key")
        )

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(createPersoon(bsn = bsn))
        )

        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = configWithApiKey,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("retrievePersoon is called") {
            localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("API key header is set on context headers") {
                localContext.headers["x-api-key"] shouldBe apiKey
            }
        }
    }

    given("An empty userName is passed (no real logged-in user available)") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val systemUser = "fakeSystemUser"
        val configWithSystemUser = createBrpConfiguration(systemUser = Optional.of(systemUser))

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = configWithSystemUser,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("retrievePersoon is called with an empty userName") {
            localService.retrievePersoon(bsn, zaaktypeUuid, "")

            then("BRP_SYSTEM_USER is used as the gebruiker header value") {
                localContext.headers["x-gebruiker"] shouldBe systemUser
            }
        }
    }

    given("Verwerkingregister extended with zaaktype is enabled") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val processingValue = "Leerplicht"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                raadpleegWaarde = "raadpleegWaarde"
                verwerkingregisterWaarde = processingValue
            }
        )
        val configWithZaaktypeExtension = createBrpConfiguration(verwerkingRegisterExtendedWithZaaktype = true)

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = configWithZaaktypeExtension,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("the verwerking header value is suffixed with the zaaktype omschrijving") {
                personResponse shouldBe person
                localContext.headers["x-verwerking"] shouldBe "$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"
            }
        }
    }

    given("Verwerkingregister extended with zaaktype is disabled") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val processingValue = "Leerplicht"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                raadpleegWaarde = "raadpleegWaarde"
                verwerkingregisterWaarde = processingValue
            }
        )
        val configWithoutZaaktypeExtension = createBrpConfiguration(verwerkingRegisterExtendedWithZaaktype = false)

        every {
            zaaktypeConfigurationService.findConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        val localContext = BrpProtocolleringContext()
        val localService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = configWithoutZaaktypeExtension,
            zaaktypeConfigurationService = zaaktypeConfigurationService,
            brpProtocolleringContext = localContext
        )

        `when`("find person is called with the BSN of the person") {
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, "fakeTestUser")

            then("the verwerking header value is not suffixed with the zaaktype omschrijving") {
                personResponse shouldBe person
                localContext.headers["x-verwerking"] shouldBe processingValue
            }
        }
    }
})
