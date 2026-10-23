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
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.enterprise.inject.Instance
import nl.info.zac.app.signalering.converter.RestSignaleringInstellingenConverter
import nl.info.zac.app.signalering.exception.SignaleringException
import nl.info.zac.app.signalering.model.RestSignaleringPageParameters
import nl.info.zac.app.signalering.model.createRestSignaleringTaskSummary
import nl.info.zac.app.zaak.model.createRESTZaakOverzicht
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.model.CheckedSignaleringen
import nl.info.zac.signalering.model.InvalidSignalering
import nl.info.zac.signalering.model.SignaleringType
import nl.info.zac.signalering.model.createRestSignaleringInstellingen
import nl.info.zac.signalering.model.createSignalering
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

    afterEach {
        checkUnnecessaryStub()
    }

    given("zaken signaleringen for ZAAK_OP_NAAM") {
        val signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
        val pageNumber = 0
        val pageSize = 5
        val numberOfElements = 11
        val restZaakOverzichtList = List(numberOfElements) { createRESTZaakOverzicht() }
        val restPageParameters = RestSignaleringPageParameters(
            page = pageNumber,
            rows = pageSize,
            sortField = SorteerVeld.SIGNALERING_TIJDSTIP,
            sortOrder = SorteerRichting.DESC
        )
        val loggedInUser = createLoggedInUser()

        every { signaleringService.countZakenSignaleringen(signaleringType) } returns numberOfElements.toLong()
        every {
            signaleringService.listCheckedZakenSignaleringenPage(signaleringType, restPageParameters, loggedInUser)
        } returns CheckedSignaleringen(valid = restZaakOverzichtList, invalid = emptyList())
        every { signaleringService.deleteInvalidSignaleringen(emptyList()) } returns 0
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("listing zaken signaleringen with proper page parameters") {
            val restResultaat = signaleringRestService.listZakenSignaleringen(signaleringType, restPageParameters)

            then("correct response is returned") {
                restResultaat.resultCount shouldBe numberOfElements
                restResultaat.results shouldBe restZaakOverzichtList
            }
        }

        `when`("listing zaken signaleringen with incorrect page parameters") {
            val exception = shouldThrow<SignaleringException> {
                signaleringRestService.listZakenSignaleringen(
                    signaleringType,
                    RestSignaleringPageParameters(
                        page = 123,
                        rows = 456,
                        sortField = SorteerVeld.SIGNALERING_TIJDSTIP,
                        sortOrder = SorteerRichting.DESC
                    )
                )
            }

            then("exception is thrown") {
                exception.message shouldBe "Requested page 123 must be <= 1"
            }
        }
    }

    given("zaken signaleringen for ZAAK_OP_NAAM of which the listed page contains two invalid ones") {
        val signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
        val restPageParameters = RestSignaleringPageParameters(
            page = 0,
            rows = 5,
            sortField = SorteerVeld.SIGNALERING_TIJDSTIP,
            sortOrder = SorteerRichting.DESC
        )
        val restZaakOverzichtList = List(3) { createRESTZaakOverzicht() }
        val invalidSignaleringen = List(2) { InvalidSignalering(signalering = createSignalering(), reason = "fakeReason") }
        val loggedInUser = createLoggedInUser()
        every { loggedInUserInstance.get() } returns loggedInUser
        every { signaleringService.countZakenSignaleringen(signaleringType) } returnsMany listOf(11L, 9L)
        every {
            signaleringService.listCheckedZakenSignaleringenPage(signaleringType, restPageParameters, loggedInUser)
        } returns CheckedSignaleringen(valid = restZaakOverzichtList, invalid = invalidSignaleringen)
        every { signaleringService.deleteInvalidSignaleringen(invalidSignaleringen) } returns 2

        `when`("listing zaken signaleringen") {
            val restResultaat = signaleringRestService.listZakenSignaleringen(signaleringType, restPageParameters)

            then("only the valid zaken are returned") {
                restResultaat.results shouldBe restZaakOverzichtList
            }
            and("the invalid signaleringen are deleted before the total is counted, so it leaves them out") {
                verifyOrder {
                    signaleringService.countZakenSignaleringen(signaleringType)
                    signaleringService.deleteInvalidSignaleringen(invalidSignaleringen)
                    signaleringService.countZakenSignaleringen(signaleringType)
                }
                restResultaat.resultCount shouldBe 9
            }
        }
    }

    given("taken signaleringen for TAAK_OP_NAAM of which one is invalid") {
        val restSignaleringTaskSummary = createRestSignaleringTaskSummary()
        val invalidSignaleringen = listOf(InvalidSignalering(signalering = createSignalering(), reason = "fakeReason"))
        every {
            signaleringService.listCheckedTakenSignaleringen(SignaleringType.Type.TAAK_OP_NAAM)
        } returns CheckedSignaleringen(valid = listOf(restSignaleringTaskSummary), invalid = invalidSignaleringen)
        every { signaleringService.deleteInvalidSignaleringen(invalidSignaleringen) } returns 1

        `when`("listing taken signaleringen") {
            val restSignaleringTaskSummaries = signaleringRestService.listTakenSignaleringen(
                SignaleringType.Type.TAAK_OP_NAAM
            )

            then("only the valid taak is returned and the invalid signalering is deleted") {
                restSignaleringTaskSummaries shouldBe listOf(restSignaleringTaskSummary)
                verify(exactly = 1) { signaleringService.deleteInvalidSignaleringen(invalidSignaleringen) }
            }
        }
    }

    given("zaken signaleringen for ZAAK_OP_NAAM with sort parameters") {
        val signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
        val numberOfElements = 7
        val restZaakOverzichtList = List(numberOfElements) { createRESTZaakOverzicht() }
        val sortedPageParameters = RestSignaleringPageParameters(
            page = 0,
            rows = 5,
            sortField = SorteerVeld.ZAAK_IDENTIFICATIE,
            sortOrder = SorteerRichting.ASC
        )
        val loggedInUser = createLoggedInUser()

        every { signaleringService.countZakenSignaleringen(signaleringType) } returns numberOfElements.toLong()
        every {
            signaleringService.listCheckedZakenSignaleringenPage(signaleringType, sortedPageParameters, loggedInUser)
        } returns CheckedSignaleringen(valid = restZaakOverzichtList, invalid = emptyList())
        every { signaleringService.deleteInvalidSignaleringen(emptyList()) } returns 0
        every { loggedInUserInstance.get() } returns loggedInUser

        `when`("listing zaken signaleringen with the sort parameters") {
            val restResultaat = signaleringRestService.listZakenSignaleringen(signaleringType, sortedPageParameters)

            then("the sort parameters are passed through unchanged and the response is returned") {
                restResultaat.resultCount shouldBe numberOfElements
                restResultaat.results shouldBe restZaakOverzichtList
            }
        }
    }

    given("An existing group and existing group signalering instellingen and a user with 'beheren' permissions") {
        val groupId = "fakeGroupId"
        val group = createGroup()
        val signaleringInstellingen = listOf(createSignaleringInstellingen())
        val restSignaleringInstellingen = listOf(createRestSignaleringInstellingen())

        every { policyService.readOverigeRechten().canBeheren } returns true
        every { identityService.readGroup(groupId, any()) } returns group
        every { signaleringService.listInstellingenInclusiefMogelijke(any()) } returns signaleringInstellingen
        every { restSignaleringInstellingenConverter.convert(signaleringInstellingen) } returns restSignaleringInstellingen

        `when`("listGroupSignaleringInstellingen is called") {
            val result = signaleringRestService.listGroupSignaleringInstellingen(groupId)

            then("it should return the list of signalering instellingen for the group") {
                result shouldBe restSignaleringInstellingen
            }
        }
    }

    given("An existing group and a user with 'beheren' permissions") {
        val groupId = "valid-group-id"
        val group = createGroup()
        val signaleringInstellingen = createSignaleringInstellingen()
        val restSignaleringInstellingen = createRestSignaleringInstellingen()

        every { policyService.readOverigeRechten().canBeheren } returns true
        every { identityService.readGroup(groupId, any()) } returns group
        every {
            restSignaleringInstellingenConverter.convert(restSignaleringInstellingen, group)
        } returns signaleringInstellingen
        every { signaleringService.createUpdateOrDeleteInstellingen(signaleringInstellingen) } returns signaleringInstellingen

        `when`("updateGroupSignaleringInstellingen is called") {
            val result = signaleringRestService.updateGroupSignaleringInstellingen(groupId, restSignaleringInstellingen)

            then("it should update the signalering instellingen for the group and return the updated instellingen") {
                result shouldBe signaleringInstellingen
            }
        }
    }

    given("A non-existing group") {
        val groupId = "invalid-group-id"
        val restSignaleringInstellingen = createRestSignaleringInstellingen()
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { identityService.readGroup(groupId, any()) } throws IllegalArgumentException("Group not found")

        `when`("listGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                signaleringRestService.listGroupSignaleringInstellingen(groupId)
            }

            then("it should throw an IllegalArgumentException") {
                exception.message shouldBe "Group not found"
            }
        }

        `when`("updateGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<IllegalArgumentException> {
                signaleringRestService.updateGroupSignaleringInstellingen(groupId, restSignaleringInstellingen)
            }

            then("it should throw an IllegalArgumentException") {
                exception.message shouldBe "Group not found"
            }
        }
    }

    given("The user does not have the required 'beheren' permissions") {
        val groupId = "fakeGroupId"
        val restSignaleringInstellingen = createRestSignaleringInstellingen()
        every { policyService.readOverigeRechten().canBeheren } returns false

        `when`("listGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<PolicyException> {
                signaleringRestService.listGroupSignaleringInstellingen(groupId)
            }

            then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }

        `when`("updateGroupSignaleringInstellingen is called") {
            val exception = shouldThrow<PolicyException> {
                signaleringRestService.updateGroupSignaleringInstellingen(groupId, restSignaleringInstellingen)
            }
            then("it should throw a PolicyException") {
                exception shouldNotBe null
            }
        }
    }
})
