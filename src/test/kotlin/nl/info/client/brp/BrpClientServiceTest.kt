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
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.BrpDoelbindingen
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.model.createZaakafhandelParameters
import java.util.Optional

const val ZAAK = "ZAAK-2000-00002"
const val CONTEXT = "$ZAAK-134"
const val ACTION = "E-mail verzenden"
const val QUERY_PERSONEN_PURPOSE = "testQueryPurpose"
const val RETRIEVE_PERSOON_PURPOSE = "testRetrievePurpose"
const val REQUEST_CONTEXT = "$CONTEXT@$ACTION"

class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val zrcClientService: ZrcClientService = mockk()
    val zaakafhandelParameterService: ZaakafhandelParameterService = mockk()
    val configuredBrpClientService = BrpClientService(
        personenApi,
        Optional.of(QUERY_PERSONEN_PURPOSE),
        Optional.of(RETRIEVE_PERSOON_PURPOSE),
        zrcClientService,
        zaakafhandelParameterService
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
        val zaakafhandelParameters = createZaakafhandelParameters(
            brpDoelbindingen = BrpDoelbindingen().apply {
                raadpleegWaarde = retrievePersoonPurpose
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaak.zaaktype.extractUuid())
        } returns zaakafhandelParameters
        every {
            personenApi.personen(
                any(),
                eq(retrievePersoonPurpose),
                eq(REQUEST_CONTEXT)
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, REQUEST_CONTEXT)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("No person for a given BSN") {
        every {
            personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, REQUEST_CONTEXT)
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = emptyList())

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", REQUEST_CONTEXT)

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
            personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, REQUEST_CONTEXT)
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon("123456789", REQUEST_CONTEXT)

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
        val zaakafhandelParameters = createZaakafhandelParameters(
            brpDoelbindingen = BrpDoelbindingen().apply {
                zoekWaarde = queryPersonenPurpose
            }
        )

        every {
            zrcClientService.readZaakByID(ZAAK)
        } returns zaak
        every {
            zaakafhandelParameterService.readZaakafhandelParameters(zaak.zaaktype.extractUuid())
        } returns zaakafhandelParameters
        every {
            personenApi.personen(
                any(),
                eq(queryPersonenPurpose),
                eq(REQUEST_CONTEXT)
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val personResponse = configuredBrpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                false,
                REQUEST_CONTEXT
            )

            Then("it should return the person") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
            }
        }
    }

    Given("No purpose is configured for BRP search") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            queryPersonenDefaultPurpose = Optional.empty(),
            retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
            zrcClientService = zrcClientService,
            zaakafhandelParameterService = zaakafhandelParameterService
        )

        every {
            personenApi.personen(any(), null, REQUEST_CONTEXT)
        } returns raadpleegMetBurgerservicenummerResponse

        When("queryPersonen is called") {
            val personResponse = brpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                false,
                REQUEST_CONTEXT
            )

            Then("it should return the person") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
            }
        }
    }

    Given("a BSN lookup is performed, the 'raadpleegWaarde' purpose is used") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            queryPersonenDefaultPurpose = Optional.empty(),
            retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE),
            zrcClientService = zrcClientService,
            zaakafhandelParameterService = zaakafhandelParameterService
        )

        every {
            personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, REQUEST_CONTEXT)
        } returns raadpleegMetBurgerservicenummerResponse

        When("queryPersonen is called") {
            val personResponse = brpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                true,
                REQUEST_CONTEXT
            )

            Then("it should return the person") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
            }
        }
    }

    Given("No purpose is configured for BRP person retrieval") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        val brpClientService = BrpClientService(
            personenApi = personenApi,
            queryPersonenDefaultPurpose = Optional.of(QUERY_PERSONEN_PURPOSE),
            retrievePersoonDefaultPurpose = Optional.empty(),
            zrcClientService = zrcClientService,
            zaakafhandelParameterService = zaakafhandelParameterService
        )

        every {
            personenApi.personen(any(), null, REQUEST_CONTEXT)
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, REQUEST_CONTEXT)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("A person exists for a given BSN") {
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
                eq(RETRIEVE_PERSOON_PURPOSE),
                eq(REQUEST_CONTEXT)
            )
        } returns raadpleegMetBurgerservicenummerResponse

        When("no zaak is found for the given audit event and retrieve persoon is called") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, REQUEST_CONTEXT)

            Then("retrieving a person should still work") {
                personResponse shouldBe person
            }
        }
    }
})
