/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.signalering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.enterprise.inject.Instance
import net.atos.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import nl.info.zac.app.signalering.exception.SignaleringException
import nl.info.zac.app.zaak.model.createRESTZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.createRestSignaleringInstellingen
import nl.info.zac.signalering.model.createSignaleringInstellingen

class SignaleringRestServiceTest : BehaviorSpec({
    val signaleringService = mockk<SignaleringService>()
    val identityService = mockk<IdentityService>()
    val policyService = mockk<PolicyService>()
    val restSignaleringInstellingenConverter = mockk<RestSignaleringInstellingenConverter>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val signaleringRestService = SignaleringRestService(
        signaleringService = signaleringService,
        identityService = identityService,
        policyService = policyService,
        restSignaleringInstellingenConverter = restSignaleringInstellingenConverter,
        loggedInUserInstance = loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("zaken signaleringen for ZAAK_OP_NAAM") {
        val signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
        val pageNumber = 0
        val pageSize = 5
        val numberOfElements = 11
        val restZaakOverzichtList = List(numberOfElements) { createRESTZaakOverzicht() }
        val restPageParameters = RestPageParameters(pageNumber, pageSize)

        every { signaleringService.countZakenSignaleringen(signaleringType) } returns numberOfElements.toLong()
        every {
            signaleringService.listZakenSignaleringenPage(signaleringType, restPageParameters)
        } returns restZaakOverzichtList

        When("listing zaken signaleringen with proper page parameters") {
            val restResultaat = signaleringRestService.listZakenSignaleringen(signaleringType, restPageParameters)

            Then("correct response is returned") {
                restResultaat.totaal shouldBe numberOfElements
                restResultaat.resultaten shouldBe restZaakOverzichtList
            }
        }

        When("listing zaken signaleringen with incorrect page parameters") {
            val exception = shouldThrow<SignaleringException> {
                signaleringRestService.listZakenSignaleringen(signaleringType, RestPageParameters(123, 456))
            }

            Then("exception is thrown") {
                exception.message shouldBe "Requested page 123 must be <= 1"
            }
        }
    }

    Given("An existing group and existing group signalering instellingen and a user with 'beheren' permissions") {
        val groupId = "fakeGroupId"
        val group = createGroup()
        val signaleringInstellingen = listOf(createSignaleringInstellingen())
        val restSignaleringInstellingen = listOf(createRestSignaleringInstellingen())

        every { policyService.readOverigeRechten().beheren } returns true
        every { identityService.readGroup(groupId) } returns group
        every { signaleringService.listInstellingenInclusiefMogelijke(any()) } returns signaleringInstellingen
        every { restSignaleringInstellingenConverter.convert(signaleringInstellingen) } returns restSignaleringInstellingen

        When("listGroupSignaleringInstellingen is called") {
            val result = signaleringRestService.listGroupSignaleringInstellingen(groupId)

            Then("it should return the list of signalering instellingen for the group") {
                result shouldBe restSignaleringInstellingen
            }
        }
    }

    Given("An existing group and a user with 'beheren' permissions") {
        val groupId = "valid-group-id"
        val group = createGroup()
        val signaleringInstellingen = createSignaleringInstellingen()
        val restSignaleringInstellingen = createRestSignaleringInstellingen()

        every { policyService.readOverigeRechten().beheren } returns true
        every { identityService.readGroup(groupId) } returns group
        every {
            restSignaleringInstellingenConverter.convert(restSignaleringInstellingen, group)
        } returns signaleringInstellingen
        every { signaleringService.createUpdateOrDeleteInstellingen(signaleringInstellingen) } returns signaleringInstellingen

        When("updateGroupSignaleringInstellingen is called") {
            val result = signaleringRestService.updateGroupSignaleringInstellingen(groupId, restSignaleringInstellingen)

            Then("it should update the signalering instellingen for the group and return the updated instellingen") {
                result shouldBe signaleringInstellingen
            }
        }
    }

    Given("A non-existing group") {
        val groupId = "invalid-group-id"
        val restSignaleringInstellingen = createRestSignaleringInstellingen()
        every { policyService.readOverigeRechten().beheren } returns true
        every { identityService.readGroup(groupId) } throws IllegalArgumentException("Group not found")

        When("listGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                signaleringRestService.listGroupSignaleringInstellingen(groupId)
            }

            Then("it should throw an IllegalArgumentException") {
                exception.message shouldBe "Group not found"
            }
        }

        When("updateGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                signaleringRestService.updateGroupSignaleringInstellingen(groupId, restSignaleringInstellingen)
            }

            Then("it should throw an IllegalArgumentException") {
                exception.message shouldBe "Group not found"
            }
        }
    }

    Given("The user does not have the required 'beheren' permissions") {
        val groupId = "fakeGroupId"
        val restSignaleringInstellingen = createRestSignaleringInstellingen()
        every { policyService.readOverigeRechten().beheren } returns false

        When("listGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<PolicyException> {
                signaleringRestService.listGroupSignaleringInstellingen(groupId)
            }

            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }

        When("updateGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<PolicyException> {
                signaleringRestService.updateGroupSignaleringInstellingen(groupId, restSignaleringInstellingen)
            }
            Then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }
})
