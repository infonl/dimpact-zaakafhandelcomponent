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
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import java.util.Optional

const val CONTEXT = "ZAAK-2000-00002-134"
const val ACTION = "E-mail verzenden"
const val QUERY_PERSONEN_PURPOSE = "testQueryPurpose"
const val RETRIEVE_PERSOON_PURPOSE = "testRetrievePurpose"
const val PROCESS = "$CONTEXT@$ACTION"

class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    var configuredBrpClientService = BrpClientService(
        personenApi,
        Optional.of(QUERY_PERSONEN_PURPOSE),
        Optional.of(RETRIEVE_PERSOON_PURPOSE)
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
        every {
            personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, PROCESS)
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(bsn, CONTEXT, ACTION)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }

    Given("No person for a given BSN") {
        every {
            personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, PROCESS)
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = emptyList())

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(
                "123456789",
                CONTEXT,
                ACTION
            )

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
            personenApi.personen(any(), RETRIEVE_PERSOON_PURPOSE, PROCESS)
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

        When("find person is called with the BSN of the person") {
            val personResponse = configuredBrpClientService.retrievePersoon(
                "123456789",
                CONTEXT,
                ACTION
            )

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
        every {
            personenApi.personen(any(), QUERY_PERSONEN_PURPOSE, PROCESS)
        } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val personResponse = configuredBrpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                CONTEXT,
                ACTION
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
            retrievePersoonDefaultPurpose = Optional.of(RETRIEVE_PERSOON_PURPOSE)
        )

        every {
            personenApi.personen(any(), null, PROCESS)
        } returns raadpleegMetBurgerservicenummerResponse

        When("queryPersonen is called") {
            val personResponse = brpClientService.queryPersonen(
                createRaadpleegMetBurgerservicenummer(listOf(bsn)),
                CONTEXT,
                ACTION
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
            retrievePersoonDefaultPurpose = Optional.empty()
        )

        every {
            personenApi.personen(any(), null, PROCESS)
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn, CONTEXT, ACTION)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }
})
