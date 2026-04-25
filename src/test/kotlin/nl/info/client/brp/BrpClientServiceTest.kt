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
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.model.generated.PersonenQuery
import nl.info.client.brp.util.BrpProtocolleringContext
import nl.info.client.brp.util.createBrpConfiguration
import nl.info.zac.admin.model.ZaaktypeBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.util.Optional
import java.util.UUID

class BrpClientServiceTest : BehaviorSpec({
    val doelbindingZoekMetDefault = "fakeDoelbindingZoekMetDefault"
    val doelbindingRaadpleegMetDefault = "fakeDoelbindingRaadpleegMetDefault"
    val verwerkingregisterDefault = "fakeVerwerkingregisterDefault"
    val zaaktypeUuid = UUID.randomUUID()
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService = mockk()
    val brpConfiguration = createBrpConfiguration(
        doelbindingZoekMetDefault = Optional.of(doelbindingZoekMetDefault),
        doelbindingRaadpleegMetDefault = Optional.of(doelbindingRaadpleegMetDefault),
        verwerkingregisterDefault = Optional.of(verwerkingregisterDefault)
    )
    val configuredBrpClientService = BrpClientService(
        personenApi = personenApi,
        brpConfiguration = brpConfiguration,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        brpProtocolleringContext = BrpProtocolleringContext()
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A person for a given BSN") {
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
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        When("find person is called with the BSN of the person") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = brpConfiguration,
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid)
            val capturedDoelbinding = localContext.doelbinding
            val capturedVerwerking = localContext.verwerking

            Then("it should return the person and set doelbinding and verwerking in context") {
                personResponse shouldBe person
                capturedDoelbinding shouldBe retrievePersoonPurpose
                capturedVerwerking shouldBe "$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"
            }
        }
    }

    Given("No person for a given BSN") {
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = emptyList()
        )

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", zaaktypeUuid)

            Then("it should return null") {
                personResponse shouldBe null
            }
        }
    }

    Given("Multiple persons for a given BSN") {
        val persons = listOf(
            createPersoon(bsn = "123456789"),
            createPersoon(bsn = "123456789")
        )
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = persons
        )

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", zaaktypeUuid)

            Then("it should return the first person") {
                personResponse shouldBe persons[0]
            }
        }
    }

    Given("Another person for a given BSN") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)
        val queryPersonenPurpose = "zoekWaarde"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                zoekWaarde = queryPersonenPurpose
                verwerkingregisterWaarde = "Leerplicht"
            }
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = brpConfiguration,
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                zaaktypeUuid
            )
            val capturedDoelbinding = localContext.doelbinding
            val capturedVerwerking = localContext.verwerking

            Then("it should return the person and set doelbinding in context") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
                capturedDoelbinding shouldBe queryPersonenPurpose
                capturedVerwerking shouldBe "Leerplicht@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"
            }
        }
    }

    Given("Unicode processing value configured for BRP person retrieval") {
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
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        When("find person is called with the BSN of the person") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = createBrpConfiguration(),
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid)
            val capturedVerwerking = localContext.verwerking

            Then("it should still return the person with default verwerking value used for unicode processing") {
                personResponse shouldBe person
                capturedVerwerking shouldBe "$verwerkingregisterDefault@fakeZaaktypeOmschrijving"
            }
        }
    }

    Given("Processing value with whitespaces") {
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
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        When("find person is called with the BSN of the person") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = createBrpConfiguration(),
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid)
            val capturedVerwerking = localContext.verwerking

            Then("it should still return the person with trimmed processing value") {
                personResponse shouldBe person
                capturedVerwerking shouldBe "Process ing\tvalue\t with whitespaces@fakeZaaktypeOmschrijving"
            }
        }
    }

    Given("Only whitespaces for BRP in zaakafhandelparameters") {
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
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        When("find person is called with the BSN of the person") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = createBrpConfiguration(brpProtocolleringProvider = Optional.of("2Secure")),
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid)
            val capturedDoelbinding = localContext.doelbinding
            val capturedVerwerking = localContext.verwerking

            Then("it should still return the person using defaults from context") {
                personResponse shouldBe person
                capturedDoelbinding shouldBe doelbindingRaadpleegMetDefault
                capturedVerwerking shouldBe "$verwerkingregisterDefault@fakeZaaktypeOmschrijving"
            }
        }
    }

    Given("A person exists for a given BSN, but no zaaktype is found for the given audit event") {
        val bsn = "123456789"
        val person = createPersoon(bsn = bsn)

        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        When("retrieve persoon is called") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = brpConfiguration,
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid)
            val capturedDoelbinding = localContext.doelbinding
            val capturedVerwerking = localContext.verwerking

            Then("retrieving a person should still work with default doelbinding and verwerking") {
                personResponse shouldBe person
                capturedDoelbinding shouldBe doelbindingRaadpleegMetDefault
                capturedVerwerking shouldBe verwerkingregisterDefault
            }
        }
    }

    Given("A logged-in user is provided") {
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
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every { personenApi.personen(any<PersonenQuery>()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        When("find person is called with the BSN of the person") {
            val localContext = BrpProtocolleringContext()
            val localService = BrpClientService(
                personenApi = personenApi,
                brpConfiguration = brpConfiguration,
                zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
                brpProtocolleringContext = localContext
            )
            val personResponse = localService.retrievePersoon(bsn, zaaktypeUuid, userName)
            val capturedGebruikersnaam = localContext.gebruikersnaam

            Then("it should return the person and set gebruikersnaam in context") {
                personResponse shouldBe person
                capturedGebruikersnaam shouldBe userName
            }
        }
    }
})
