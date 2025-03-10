/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.model.createPersoon
import net.atos.client.brp.model.createRaadpleegMetBurgerservicenummer
import net.atos.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.BrpClientService
import nl.info.client.brp.PersonenApi

class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val brpClientService = BrpClientService(
        personenApi
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
        every { personenApi.personen(any()) } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.retrievePersoon(bsn)

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }
    Given("No person for a given BSN") {
        every { personenApi.personen(any()) } returns createRaadpleegMetBurgerservicenummerResponse(
            persons = emptyList()
        )

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
        every { personenApi.personen(any()) } returns createRaadpleegMetBurgerservicenummerResponse(persons = persons)

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
        every { personenApi.personen(any()) } returns raadpleegMetBurgerservicenummerResponse

        When("a query is run on personen for this BSN") {
            val personResponse = brpClientService.queryPersonen(createRaadpleegMetBurgerservicenummer(listOf(bsn)))

            Then("it should return the person") {
                personResponse shouldBe raadpleegMetBurgerservicenummerResponse
            }
        }
    }
})
