/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.brp.exception.BrpInvalidPurposeException
import nl.info.client.brp.model.createPersoon
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import java.util.Optional

const val PURPOSE_SEARCH = "customPurpose"
const val PURPOSE_RETRIEVE = "customRetrieve"

@Suppress("NAME_SHADOWING")
class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val brpClientService = BrpClientService(
        personenApi,
        Optional.of(PURPOSE_SEARCH),
        Optional.of(PURPOSE_RETRIEVE)
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
            personenApi.personen(any(), PURPOSE_RETRIEVE)
        } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }
    Given("No person for a given BSN") {
        every {
            personenApi.personen(any(), PURPOSE_RETRIEVE)
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = emptyList())

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon("123456789")

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
            personenApi.personen(any(), PURPOSE_RETRIEVE)
        } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon("123456789")

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
            personenApi.personen(any(), PURPOSE_SEARCH)
        } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val personResponse = brpClientService.queryPersonen(createRaadpleegMetBurgerservicenummer(listOf(bsn)))

            Then("it should return the person") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
            }
        }
    }
    Given("No purpose is configured for BRP search") {
        val personenQuery = createRaadpleegMetBurgerservicenummer(listOf("123456789"))

        val brpClientService = BrpClientService(
            personenApi = personenApi,
            purposeSearch = Optional.empty(),
            purposeRetrieve = Optional.of(PURPOSE_RETRIEVE)
        )

        When("queryPersonen is called") {
            Then("it should throw BrpInvalidPurposeException") {
                shouldThrow<BrpInvalidPurposeException> {
                    brpClientService.queryPersonen(personenQuery)
                }.message shouldBe "brp.doelbinding.zoekmet must be configured and not empty."
            }
        }
    }
    Given("Blank purpose is configured for BRP search") {
        val personenQuery = createRaadpleegMetBurgerservicenummer(listOf("123456789"))

        val brpClientService = BrpClientService(
            personenApi = personenApi,
            purposeSearch = Optional.of(" "),
            purposeRetrieve = Optional.of(PURPOSE_RETRIEVE)
        )

        When("queryPersonen is called") {
            Then("it should throw BrpInvalidPurposeException") {
                shouldThrow<BrpInvalidPurposeException> {
                    brpClientService.queryPersonen(personenQuery)
                }.message shouldBe "brp.doelbinding.zoekmet must not be blank."
            }
        }
    }
})
