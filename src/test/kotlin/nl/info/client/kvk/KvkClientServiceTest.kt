/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.kvk.exception.KvkClientNoResultException
import nl.info.client.kvk.model.KvkSearchParameters
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.kvk.vestigingsprofiel.model.generated.Vestiging
import nl.info.client.kvk.zoeken.model.generated.Resultaat

class KvkClientServiceTest : BehaviorSpec({
    val kvkSearchClient = mockk<KvkSearchClient>()
    val kvkVestigingsprofielClient = mockk<KvkVestigingsprofielClient>()
    val kvkClientService = KvkClientService(
        kvkSearchClient = kvkSearchClient,
        kvkVestigingsprofielClient = kvkVestigingsprofielClient
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("KVK search parameters for which the KVK client returns results") {
        val resultaatItem = createResultaatItem()
        val resultaat = Resultaat().apply {
            totaal = 1
            resultaten = listOf(resultaatItem)
        }
        val kvkSearchParameters = KvkSearchParameters().apply {
            vestigingsnummer = "12345678"
        }
        every { kvkSearchClient.getResults(kvkSearchParameters) } returns resultaat

        When("search is called with these KVK search parameters") {
            val result = kvkClientService.search(kvkSearchParameters)

            Then("it should return the expected results from the KVK client") {
                with(result) {
                    totaal shouldBe 1
                    resultaten.size shouldBe 1
                    resultaten.first() shouldBe resultaatItem
                }
            }
        }
    }

    Given("KVK search parameters for which the KVK client throws an exception indicating there are no results") {
        val kvkSearchParameters = KvkSearchParameters().apply {
            vestigingsnummer = "12345678"
        }
        every { kvkSearchClient.getResults(kvkSearchParameters) } throws KvkClientNoResultException("not found!")

        When("search is called and the client throws KvkClientNoResultException") {
            val result = kvkClientService.search(kvkSearchParameters)

            Then("it should return an empty Resultaat object") {
                result.totaal shouldBe 0
            }
        }
    }

    Given("A vestiging with a vestigingsnummer") {
        val vestigingsnummer = "12345678"
        val expectedVestiging = Vestiging()
        every { kvkVestigingsprofielClient.getVestigingByVestigingsnummer(vestigingsnummer, false) } returns expectedVestiging

        When("the vestigingsprofiel is retrieved for this vestigingsnummer") {
            val result = kvkClientService.findVestigingsprofiel(vestigingsnummer)

            Then("it should return the expected vestiging") {
                result shouldBe expectedVestiging
            }
        }
    }
})
