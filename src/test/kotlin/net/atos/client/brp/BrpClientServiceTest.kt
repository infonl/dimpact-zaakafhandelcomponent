/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.exception.BrpPersonNotFoundException
import net.atos.client.brp.model.createPersoon
import net.atos.client.brp.model.createRaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.PersonenQueryResponse
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.BiFunction

class BrpClientServiceTest : BehaviorSpec({
    val personenApi: PersonenApi = mockk<PersonenApi>()
    val brpClientService = BrpClientService(
        personenApi
    )
    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A person") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val raadpleegMetBurgerservicenummerResponse = createRaadpleegMetBurgerservicenummerResponse(
            persons = listOf(person)
        )
        every { personenApi.personen(any()) } returns raadpleegMetBurgerservicenummerResponse

        When("find person is called with the BSN of the person") {
            val personResponse = brpClientService.findPersoon(bsn)

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
            val personResponse = brpClientService.findPersoon("123456789")

            Then("it should return null") {
                personResponse shouldBe null
            }
        }
    }
    Given("Another person") {
        val bsn = "123456789"
        val person = createPersoon(
            bsn = bsn
        )
        val completionStage = mockk<CompletionStage<PersonenQueryResponse>>()
        val completionStageResponse = CompletableFuture.completedFuture(person)
        every { personenApi.personenAsync(any()) } returns completionStage
        every {
            completionStage.handle(any<BiFunction<PersonenQueryResponse, Throwable?, Any>>())
        } returns (completionStageResponse as CompletionStage<Any>)

        When("async find person is called with the BSN of the person") {
            val personResponse = brpClientService.findPersoonAsync(bsn).toCompletableFuture().get()

            Then("it should return the person") {
                personResponse shouldBe person
            }
        }
    }
    Given("No person for a given BSN for async") {
        val bsn = "123456789"
        val completionStage = mockk<CompletionStage<PersonenQueryResponse>>()
        every { personenApi.personenAsync(any()) } returns completionStage
        every {
            completionStage.handle(any<BiFunction<PersonenQueryResponse, Throwable?, Any>>())
        } throws BrpPersonNotFoundException("Terrible error!")

        When("async find person is called with the BSN of the person") {
            val exception = shouldThrow<BrpPersonNotFoundException> {
                brpClientService.findPersoonAsync(bsn).toCompletableFuture().get()
            }

            Then("an exception should be thrown") {
                with(exception) {
                    message shouldBe "Terrible error!"
                }
            }
        }
    }
})
