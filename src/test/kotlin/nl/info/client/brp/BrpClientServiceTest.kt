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
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
    )
    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A person for a given BSN") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
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
        every {
            personenApi.personen(
                any<PersonenQuery>(),
                eq(retrievePersoonPurpose),
                eq("$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"),
                null
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, zaaktypeUuid)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("No person for a given BSN") {
        every {
            personenApi.personen(
                personenQuery = any(),
                doelbinding = doelbindingRaadpleegMetDefault,
                verwerking = verwerkingregisterDefault,
                gebruikersnaam = null
            )
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = emptyList())

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
            personenApi.personen(
                personenQuery = any(),
                doelbinding = doelbindingRaadpleegMetDefault,
                verwerking = verwerkingregisterDefault,
                gebruikersnaam = null
            )
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", zaaktypeUuid)

            Then("it should return the first person") {
                personResponse shouldBe persons[0]
            }
        }
    }

    Given("Another person for a given BSN") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val queryPersonenPurpose = "zoekWaarde"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeBrpParameters = ZaaktypeBrpParameters().apply {
                zoekWaarde = queryPersonenPurpose
                verwerkingregisterWaarde = "Leerplicht"
            }
        )

        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every {
            personenApi.personen(
                any(),
                eq(queryPersonenPurpose),
                eq("Leerplicht@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"),
                null
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val personResponse = configuredBrpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                zaaktypeUuid
            )

            Then("it should return the person") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
            }
        }
    }

    Given("Unicode processing value configured for BRP person retrieval") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val brpConfiguration = createBrpConfiguration()
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )
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
        every {
            // Since we have a processing value in Unicode, the default value is used instead
            personenApi.personen(
                personenQuery = any(),
                doelbinding = retrievePersoonPurpose,
                verwerking = "$verwerkingregisterDefault@fakeZaaktypeOmschrijving",
                gebruikersnaam = null
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, zaaktypeUuid)

            Then("it should still return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("Processing value with whitespaces") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val brpConfiguration = createBrpConfiguration()
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )
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
        every {
            // Since we have a whitespace prefix and suffix, the value is trimmed
            personenApi.personen(any(), retrievePersoonPurpose, "Process ing\tvalue\t with whitespaces@fakeZaaktypeOmschrijving", null)
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, zaaktypeUuid)

            Then("it should still return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("Only whitespaces for BRP in zaakafhandelparameters") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val brpConfiguration = createBrpConfiguration(
            brpProtocolleringProvider = Optional.of("2Secure")
        )
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )
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
        every {
            // We have no zaakafhandelparameter values, so the defaults are used instead
            personenApi.personen(
                personenQuery = any(),
                doelbinding = doelbindingRaadpleegMetDefault,
                verwerking = "$verwerkingregisterDefault@fakeZaaktypeOmschrijving",
                gebruikersnaam = null
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, zaaktypeUuid)

            Then("it should still return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("A person exists for a given BSN, but no zaaktype is found for the given audit event ") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid)
        } throws NotFoundException("Zaak not found")

        every {
            personenApi.personen(
                personenQuery = any(),
                doelbinding = doelbindingRaadpleegMetDefault,
                verwerking = verwerkingregisterDefault,
                gebruikersnaam = null
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("retrieve persoon is called") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, zaaktypeUuid)

            Then("retrieving a person should still work") {
                personResponse shouldBe person
            }
        }
    }

    Given("A logged-in user is provided") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

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
        every {
            personenApi.personen(
                any<PersonenQuery>(),
                eq(retrievePersoonPurpose),
                eq("$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}"),
                userName
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, zaaktypeUuid, userName)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }
})
