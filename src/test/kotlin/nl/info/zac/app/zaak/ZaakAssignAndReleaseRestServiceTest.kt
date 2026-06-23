/*
 * SPDX-FileCopyrightText: 2023 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.model.createRESTZaakAssignmentData
import nl.info.zac.app.zaak.model.createRESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.createRESTZakenVrijgevenGegevens
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakAssignmentToLoggedInUserData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.zaak.ZaakService
import java.util.UUID

class ZaakAssignAndReleaseRestServiceTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val zaakService = mockk<ZaakService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakAssignAndReleaseRestService = ZaakAssignAndReleaseRestService(
        dispatcher = testDispatcher,
        identityService = identityService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        restZaakConverter = restZaakConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        zaakService = zaakService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    Context("Assigning zaken from a list") {
        Given("REST zaken verdeel gegevens with a group and a user") {
            val zaakUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
            val group = createGroup()
            val user = createUser()
            val restZakenVerdeelGegevens = createRESTZakenVerdeelGegevens(
                uuids = zaakUUIDs,
                groepId = group.name,
                behandelaarGebruikersnaam = user.id,
                reden = "fakeReason"
            )
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
            every { zaakService.assignZaken(any(), any(), any(), any(), any()) } just runs
            every { identityService.readGroup(group.name) } returns group
            every { identityService.readUser(restZakenVerdeelGegevens.behandelaarGebruikersnaam!!) } returns user

            When("the assign zaken from a list function is called") {
                runTest(testDispatcher) {
                    zaakAssignAndReleaseRestService.assignFromList(restZakenVerdeelGegevens)
                }

                Then("the zaken are assigned to the group and user") {
                    verify(exactly = 1) {
                        zaakService.assignZaken(
                            zaakUUIDs,
                            group,
                            user,
                            restZakenVerdeelGegevens.reden,
                            restZakenVerdeelGegevens.screenEventResourceId
                        )
                    }
                }
            }
        }
    }

    Context("Assigning a zaak") {
        Given("zaak assignment data is provided") {
            val restZaakAssignmentData = createRESTZaakAssignmentData()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaak = createRestZaak()
            val loggedInUser = createLoggedInUser()

            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID) } returns Pair(zaak, zaakType)
            every {
                zaakService.assignZaak(
                    zaak,
                    restZaakAssignmentData.groupId,
                    restZaakAssignmentData.assigneeUserName,
                    restZaakAssignmentData.reason
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, any(), loggedInUser) } returns restZaak
            every { loggedInUserInstance.get() } returns loggedInUser

            When("toekennen policy is assigned to the user") {
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)

                Then("expected response is prepared") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            When("toekennen policy is missing") {
                every {
                    policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)
                }

                Then("exception is thrown") {}
            }
        }
    }

    Context("Assigning a zaak to the logged-in user") {
        Given("when zaak is open and toekennen policy is assigned to the logged-in user") {
            val restZaakAssignmentToLoggedInUserData = createRestZaakAssignmentToLoggedInUserData()
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaak = createRestZaak()

            val loggedInUserId = "loggedInUserId"
            val loggedInUser = createLoggedInUser(id = loggedInUserId)
            every { loggedInUserInstance.get() } returns loggedInUser

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
            } returns Pair(zaak, zaakType)
            every {
                zaakService.assignZaak(
                    zaak,
                    restZaakAssignmentToLoggedInUserData.groupId,
                    loggedInUserId,
                    restZaakAssignmentToLoggedInUserData.reason
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, any(), loggedInUser) } returns restZaak

            When("toekennen policy is assigned to the logged-in user") {
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(
                    restZaakAssignmentToLoggedInUserData
                )

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            When("logged-in user does not have toekennen policy") {
                every {
                    policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)
                }

                Then("exception is thrown") {}
            }
        }
    }

    Context("Assigning a closed zaak to the logged-in user") {
        Given("when zaak is closed and toekennen policy is assigned to the logged-in user") {
            val restZaakAssignmentToLoggedInUserData = createRestZaakAssignmentToLoggedInUserData()
            val zaak = createZaak()
            zaak.archiefnominatie = ArchiefnominatieEnum.VERNIETIGEN
            val zaakType = createZaakType()
            val restZaak = createRestZaak()

            val loggedInUserId = "loggedInUserId"
            val loggedInUser = createLoggedInUser(id = loggedInUserId)
            every { loggedInUserInstance.get() } returns loggedInUser

            every {
                zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentToLoggedInUserData.zaakUUID)
            } returns Pair(zaak, zaakType)
            every {
                zaakService.assignZaak(
                    zaak,
                    restZaakAssignmentToLoggedInUserData.groupId,
                    loggedInUserId,
                    restZaakAssignmentToLoggedInUserData.reason
                )
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, any(), loggedInUser) } returns restZaak

            When("toekennen policy is assigned to the logged-in user") {
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(
                    restZaakAssignmentToLoggedInUserData
                )

                Then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            When("logged-in user does not have toekennen policy") {
                every {
                    policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)
                }

                Then("exception is thrown") {}
            }
        }
    }

    Context("Releasing zaken from a list") {
        Given("REST zaken vrijgeven gegevens and a user with the 'zaken taken verdelen' permission") {
            val zaakUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
            val restZakenVrijgevenGegevens = createRESTZakenVrijgevenGegevens(
                uuids = zaakUUIDs,
                reden = "fakeReason",
                screenEventResourceId = "fakeScreenEventResourceId"
            )
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
            every { zaakService.releaseZaken(any(), any(), any()) } just runs

            When("the release zaken from a list function is called") {
                runTest(testDispatcher) {
                    zaakAssignAndReleaseRestService.releaseZakenFromList(restZakenVrijgevenGegevens)
                }

                Then("the zaken are released") {
                    verify(exactly = 1) {
                        zaakService.releaseZaken(
                            zaakUUIDs,
                            restZakenVrijgevenGegevens.reden,
                            restZakenVrijgevenGegevens.screenEventResourceId
                        )
                    }
                }
            }
        }

        Given("REST zaken vrijgeven gegevens and a user without the 'zaken taken verdelen' permission") {
            val restZakenVrijgevenGegevens = createRESTZakenVrijgevenGegevens(
                uuids = listOf(UUID.randomUUID())
            )
            every { policyService.readWerklijstRechten() } returns createWerklijstRechtenAllDeny()

            When("the release zaken from a list function is called") {
                Then("a policy exception is thrown and no zaken are released") {
                    shouldThrow<PolicyException> {
                        zaakAssignAndReleaseRestService.releaseZakenFromList(restZakenVrijgevenGegevens)
                    }
                    verify(exactly = 0) {
                        zaakService.releaseZaken(any(), any(), any())
                    }
                }
            }
        }
    }
})
