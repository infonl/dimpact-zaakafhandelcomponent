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
import java.util.UUID
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.info.client.zgw.model.createMedewerkerIdentificatie
import nl.info.client.zgw.model.createRolMedewerker
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakEigenschap
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.app.zaak.converter.RestZaakConverter
import nl.info.zac.app.zaak.converter.RestZaakOverzichtConverter
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException
import nl.info.zac.app.zaak.exception.ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException
import nl.info.zac.app.zaak.model.createRESTZaakAssignmentData
import nl.info.zac.app.zaak.model.createRESTZakenVerdeelGegevens
import nl.info.zac.app.zaak.model.createRESTZakenVrijgevenGegevens
import nl.info.zac.app.zaak.model.createRestZaak
import nl.info.zac.app.zaak.model.createRestZaakAssignmentToLoggedInUserData
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.exception.ErrorCode
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createWerklijstRechten
import nl.info.zac.policy.output.createWerklijstRechtenAllDeny
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.zaak.ZaakService

class ZaakAssignAndReleaseRestServiceTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val policyService = mockk<PolicyService>()
    val restZaakConverter = mockk<RestZaakConverter>()
    val restZaakOverzichtConverter = mockk<RestZaakOverzichtConverter>()
    val zaakService = mockk<ZaakService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val testDispatcher = StandardTestDispatcher()
    val zaakAssignAndReleaseRestService = ZaakAssignAndReleaseRestService(
        dispatcher = testDispatcher,
        identityService = identityService,
        loggedInUserInstance = loggedInUserInstance,
        policyService = policyService,
        restZaakConverter = restZaakConverter,
        restZaakOverzichtConverter = restZaakOverzichtConverter,
        zaakService = zaakService,
        zgwApiService = zgwApiService,
        zrcClientService = zrcClientService
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Assigning zaken from a list") {
        given("REST zaken verdeel gegevens with a group and a user") {
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

            `when`("the assign zaken from a list function is called") {
                runTest(testDispatcher) {
                    zaakAssignAndReleaseRestService.assignFromList(restZakenVerdeelGegevens)
                }

                then("the zaken are assigned to the group and user") {
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

    context("Assigning a zaak") {
        given("zaak assignment data is provided") {
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
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

            `when`("toekennen policy is assigned to the user") {
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)

                then("expected response is prepared") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            `when`("toekennen policy is missing") {
                every {
                    policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)
                }

                then("exception is thrown") {}
            }
        }
    }

    context("Assigning a zaak to the logged-in user") {
        given("when zaak is open and toekennen policy is assigned to the logged-in user") {
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
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

            `when`("toekennen policy is assigned to the logged-in user") {
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(
                    restZaakAssignmentToLoggedInUserData
                )

                then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            `when`("logged-in user does not have toekennen policy") {
                every {
                    policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)
                }

                then("exception is thrown") {}
            }
        }
    }

    context("Assigning a closed zaak to the logged-in user") {
        given("when zaak is closed and toekennen policy is assigned to the logged-in user") {
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
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns emptyList()

            `when`("toekennen policy is assigned to the logged-in user") {
                every { policyService.readZaakRechten(zaak, zaakType, loggedInUser) } returns createZaakRechtenAllDeny(toekennen = true)
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(
                    restZaakAssignmentToLoggedInUserData
                )

                then("the zaak is assigned both to the group and the user") {
                    returnedRestZaak shouldBe restZaak
                }
            }

            `when`("logged-in user does not have toekennen policy") {
                every {
                    policyService.readZaakRechten(zaak, zaakType, loggedInUser)
                } returns createZaakRechtenAllDeny(toekennen = false)
                shouldThrow<PolicyException> {
                    zaakAssignAndReleaseRestService.assignZaakToLoggedInUser(restZaakAssignmentToLoggedInUserData)
                }

                then("exception is thrown") {}
            }
        }
    }

    context("Assigning or releasing a zaakspecifiek geautoriseerde zaak") {
        given("a zaakspecifiek geautoriseerde zaak and assignment data naming a different behandelaar") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val restZaakAssignmentData = createRESTZaakAssignmentData(behandelaarGebruikersnaam = "fakeOtherUserId")

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny(toekennen = true)
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
            )
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns createRolMedewerker(
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId")
            )

            `when`("the zaak is assigned") {
                val exception = shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReassignedException> {
                    zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)
                }

                then("the attempt is refused with its own error code and the zaak keeps its behandelaar") {
                    exception.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_REASSIGNED
                    verify(exactly = 0) { zaakService.assignZaak(any(), any(), any(), any()) }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak and assignment data without a behandelaar") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val loggedInUser = createLoggedInUser()
            val restZaakAssignmentData = createRESTZaakAssignmentData(behandelaarGebruikersnaam = null)

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny(toekennen = true)
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
            )
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns createRolMedewerker(
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId")
            )

            `when`("the zaak is released") {
                val exception = shouldThrow<ZaakspecifiekGeautoriseerdeZaakCannotBeReleasedException> {
                    zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)
                }

                then("the attempt is refused with its own error code and the zaak keeps its behandelaar") {
                    exception.errorCode shouldBe
                        ErrorCode.ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_ZAAK_CANNOT_BE_RELEASED
                    verify(exactly = 0) { zaakService.assignZaak(any(), any(), any(), any()) }
                }
            }
        }

        given("a zaakspecifiek geautoriseerde zaak and assignment data repeating its current behandelaar") {
            val zaak = createZaak()
            val zaakType = createZaakType()
            val restZaak = createRestZaak()
            val loggedInUser = createLoggedInUser()
            val restZaakAssignmentData = createRESTZaakAssignmentData(behandelaarGebruikersnaam = "fakeBehandelaarId")

            every { loggedInUserInstance.get() } returns loggedInUser
            every { zaakService.readZaakAndZaakTypeByZaakUUID(restZaakAssignmentData.zaakUUID) } returns Pair(zaak, zaakType)
            every {
                policyService.readZaakRechten(zaak, zaakType, loggedInUser)
            } returns createZaakRechtenAllDeny(toekennen = true)
            every { zrcClientService.listZaakeigenschappen(zaak.uuid) } returns listOf(
                createZaakEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD, waarde = "true")
            )
            every { zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak) } returns createRolMedewerker(
                medewerkerIdentificatie = createMedewerkerIdentificatie(identificatie = "fakeBehandelaarId")
            )
            every {
                zaakService.assignZaak(zaak, restZaakAssignmentData.groupId, "fakeBehandelaarId", restZaakAssignmentData.reason)
            } just runs
            every { restZaakConverter.toRestZaak(zaak, zaakType, any(), loggedInUser) } returns restZaak

            `when`("the zaak is assigned") {
                val returnedRestZaak = zaakAssignAndReleaseRestService.assignZaak(restZaakAssignmentData)

                then("the assignment is not refused, so that the group can still be changed") {
                    returnedRestZaak shouldBe restZaak
                }
            }
        }
    }

    context("Releasing zaken from a list") {
        given("REST zaken vrijgeven gegevens and a user with the 'zaken taken verdelen' permission") {
            val zaakUUIDs = listOf(UUID.randomUUID(), UUID.randomUUID())
            val restZakenVrijgevenGegevens = createRESTZakenVrijgevenGegevens(
                uuids = zaakUUIDs,
                reden = "fakeReason",
                screenEventResourceId = "fakeScreenEventResourceId"
            )
            every { policyService.readWerklijstRechten() } returns createWerklijstRechten()
            every { zaakService.releaseZaken(any(), any(), any()) } just runs

            `when`("the release zaken from a list function is called") {
                runTest(testDispatcher) {
                    zaakAssignAndReleaseRestService.releaseZakenFromList(restZakenVrijgevenGegevens)
                }

                then("the zaken are released") {
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

        given("REST zaken vrijgeven gegevens and a user without the 'zaken taken verdelen' permission") {
            val restZakenVrijgevenGegevens = createRESTZakenVrijgevenGegevens(
                uuids = listOf(UUID.randomUUID())
            )
            every { policyService.readWerklijstRechten() } returns createWerklijstRechtenAllDeny()

            `when`("the release zaken from a list function is called") {
                then("a policy exception is thrown and no zaken are released") {
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
