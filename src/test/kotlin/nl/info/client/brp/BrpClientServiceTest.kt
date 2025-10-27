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
import nl.info.client.brp.util.createBrpConfiguration
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.model.ZaaktypeCmmnBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import java.util.Optional

const val ZAAK = "ZAAK-2000-00002"

class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val zrcClientService: ZrcClientService = mockk()
    val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService = mockk()
    val brpConfiguration = createBrpConfiguration()
    val configuredBrpClientService = BrpClientService(
        personenApi = personenApi,
        brpConfiguration = brpConfiguration,
        zrcClientService = zrcClientService,
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
        val zaak = createZaak()
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "Leerplicht"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
        } returns zaaktypeCmmnConfiguration
        every {
            personenApi.personen(
                any(),
                eq(retrievePersoonPurpose),
                eq("$processingValue@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}")
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, ZAAK)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("No person for a given BSN") {
        every {
            personenApi.personen(any(), "retrievePersoonPurpose", "processingRegisterDefault")
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = emptyList())

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", ZAAK)

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
            personenApi.personen(any(), "retrievePersoonPurpose", "processingRegisterDefault")
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", ZAAK)

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
        val zaak = createZaak()
        val queryPersonenPurpose = "zoekWaarde"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                zoekWaarde = queryPersonenPurpose
                verwerkingregisterWaarde = "Leerplicht"
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
        } returns zaaktypeCmmnConfiguration
        every {
            personenApi.personen(
                any(),
                eq(queryPersonenPurpose),
                eq("Leerplicht@${zaaktypeCmmnConfiguration.zaaktypeOmschrijving}")
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val personResponse = configuredBrpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                ZAAK
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
            zrcClientService = zrcClientService,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )
        val zaak = createZaak()
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "Bíj́na"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
        } returns zaaktypeCmmnConfiguration
        every {
            // Since we have a processing value in Unicode, the default value is used instead
            personenApi.personen(any(), retrievePersoonPurpose, "processingRegisterDefault@fakeZaaktypeOmschrijving")
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, ZAAK)

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
            zrcClientService = zrcClientService,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )
        val zaak = createZaak()
        val retrievePersoonPurpose = "raadpleegWaarde"
        val processingValue = "  \t Process ing\tvalue\t with whitespaces \t"
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                raadpleegWaarde = retrievePersoonPurpose
                verwerkingregisterWaarde = processingValue
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
        } returns zaaktypeCmmnConfiguration
        every {
            // Since we have a whitespace prefix and suffix, the value is trimmed
            personenApi.personen(any(), retrievePersoonPurpose, "Process ing\tvalue\t with whitespaces@fakeZaaktypeOmschrijving")
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, ZAAK)

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
            auditLogProvider = Optional.of("2Secure")
        )
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            brpConfiguration = brpConfiguration,
            zrcClientService = zrcClientService,
            zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService
        )
        val zaak = createZaak()
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeCmmnBrpParameters = ZaaktypeCmmnBrpParameters().apply {
                zoekWaarde = ""
                raadpleegWaarde = ""
                verwerkingregisterWaarde = ""
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaak.zaaktype.extractUuid())
        } returns zaaktypeCmmnConfiguration
        every {
            // We have no zaakafhandelparameter values, so the defaults are used instead
            personenApi.personen(any(), "retrievePersoonPurpose", "processingRegisterDefault@fakeZaaktypeOmschrijving")
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, ZAAK)

            Then("it should still return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("A person exists for a given BSN, but no zaak is found for the given audit event ") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } throws NotFoundException("Zaak not found")

        every {
            personenApi.personen(
                any(),
                "retrievePersoonPurpose",
                "processingRegisterDefault"
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("retrieve persoon is called") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, ZAAK)

            Then("retrieving a person should still work") {
                personResponse shouldBe person
            }
        }
    }
})
