/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.helper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.checkUnnecessaryStub
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.model.createOpschorting
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.policy.output.createZaakRechtenAllDeny
import nl.info.zac.shared.helper.SuspensionZaakHelper
import java.time.LocalDate
import java.time.ZonedDateTime

class SuspensionZaakHelperTest : BehaviorSpec({
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val flowableTaskService = mockk<FlowableTaskService>()

    val suspensionZaakHelper = SuspensionZaakHelper(
        policyService,
        zrcClientService,
        zaakVariabelenService,
        flowableTaskService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context("suspend/postpone zaak") {
        Given("a zaak that is open and not yet postponed and does not have an planned end date") {
            val numberOfDaysPostponed = 123L
            val postPonementReason = "fakeReason"
            val zaak = createZaak(
                opschorting = createOpschorting(reden = null),
                einddatumGepland = null,
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1)
            )
            val postponedZaak = createZaak(
                opschorting = createOpschorting(reden = "fakeReason"),
                einddatumGepland = null,
                uiterlijkeEinddatumAfdoening = LocalDate.now()
                    .plusDays(1 + numberOfDaysPostponed)
            )
            val patchedZaak = slot<Zaak>()

            every {
                zaakVariabelenService.setDatumtijdOpgeschort(
                    zaak.uuid,
                    any()
                )
            } just runs
            every {
                zaakVariabelenService.setVerwachteDagenOpgeschort(
                    zaak.uuid,
                    numberOfDaysPostponed.toInt()
                )
            } just runs
            every {
                zrcClientService.patchZaak(
                    zaak.uuid,
                    capture(patchedZaak),
                    "Opschorting: $postPonementReason"
                )
            } returns postponedZaak

            When("the zaak is postponed for x days from user with access") {
                every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(opschorten = true)

                val returnedZaak = suspensionZaakHelper.suspendZaak(
                    zaak,
                    numberOfDaysPostponed,
                    postPonementReason
                )

                Then("the zaak should be postponed and the final date should be extended with x days") {
                    returnedZaak shouldBe postponedZaak
                    verify(exactly = 1) {
                        zaakVariabelenService.setDatumtijdOpgeschort(zaak.uuid, any())
                        zaakVariabelenService.setVerwachteDagenOpgeschort(
                            zaak.uuid,
                            numberOfDaysPostponed.toInt()
                        )
                        zrcClientService.patchZaak(
                            zaak.uuid,
                            any(),
                            "Opschorting: $postPonementReason"
                        )
                    }
                    with(patchedZaak.captured) {
                        opschorting.reden shouldBe postPonementReason
                        einddatumGepland shouldBe null
                        uiterlijkeEinddatumAfdoening shouldBe postponedZaak.uiterlijkeEinddatumAfdoening
                    }
                }
            }

            When("the zaak is postponed for x days from user with no access") {
                every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

                val exception = shouldThrow<PolicyException> {
                    suspensionZaakHelper.suspendZaak(
                        zaak,
                        numberOfDaysPostponed,
                        postPonementReason
                    )
                }

                Then("it throws exception with no message") { exception.message shouldBe null }
            }
        }
    }

    Context("Resume zaak") {
        Given("a zaak that is postponed and does not have a planned end date") {
            val reasonResumed = "fakeResumeReason"
            val zaak = createZaak(
                opschorting = createOpschorting(
                    reden = "fakePostponementReason",
                    indicatie = true
                ),
                einddatumGepland = null,
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1)
            )
            val patchedZaak = slot<Zaak>()

            every { zaakVariabelenService.findDatumtijdOpgeschort(zaak.uuid) } returns null
            every { zaakVariabelenService.findVerwachteDagenOpgeschort(zaak.uuid) } returns null
            every {
                zrcClientService.patchZaak(
                    zaak.uuid,
                    capture(patchedZaak),
                    "Hervatting: $reasonResumed"
                )
            } returns createZaak()
            every { zaakVariabelenService.removeDatumtijdOpgeschort(zaak.uuid) } just runs
            every { zaakVariabelenService.removeVerwachteDagenOpgeschort(zaak.uuid) } just runs

            When("the zaak is resumed from user with access") {
                every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(hervatten = true)

                suspensionZaakHelper.resumeZaak(zaak, reasonResumed)

                Then("the zaak should be resumed") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(
                            zaak.uuid,
                            any(),
                            "Hervatting: $reasonResumed"
                        )
                        zaakVariabelenService.removeDatumtijdOpgeschort(zaak.uuid)
                        zaakVariabelenService.removeVerwachteDagenOpgeschort(zaak.uuid)
                    }
                    with(patchedZaak.captured) {
                        opschorting.reden shouldBe reasonResumed
                        einddatumGepland shouldBe null
                        uiterlijkeEinddatumAfdoening shouldBe zaak.uiterlijkeEinddatumAfdoening
                    }
                }
            }

            When("the zaak is resumed from user with no access") {
                every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

                val exception = shouldThrow<PolicyException> { suspensionZaakHelper.resumeZaak(zaak, reasonResumed) }

                Then("it throws exception with no message") { exception.message shouldBe null }
            }
        }

        Given("a suspended zaak with a expected processing period and planned end date") {
            val reasonResumed = "fakeResumeReason"
            val zaak = createZaak(
                opschorting = createOpschorting(
                    reden = "fakePostponementReason",
                    indicatie = true
                ),
                einddatumGepland = LocalDate.now().plusDays(2),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1)
            )
            val patchedZaak = slot<Zaak>()

            // Suspended 3 days ago (on Friday, now it is Monday)
            every { zaakVariabelenService.findDatumtijdOpgeschort(zaak.uuid) } returns ZonedDateTime.now().minusDays(3)
            // One day needed for processing
            every { zaakVariabelenService.findVerwachteDagenOpgeschort(zaak.uuid) } returns 1
            every {
                zrcClientService.patchZaak(
                    zaak.uuid,
                    capture(patchedZaak),
                    "Hervatting: $reasonResumed"
                )
            } returns createZaak()
            every { zaakVariabelenService.removeDatumtijdOpgeschort(zaak.uuid) } just runs
            every { zaakVariabelenService.removeVerwachteDagenOpgeschort(zaak.uuid) } just runs
            every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(hervatten = true)

            When("the zaak is resumed with default resume date (today)") {
                suspensionZaakHelper.resumeZaak(zaak, reasonResumed)

                Then("the zaak should be resumed") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(
                            zaak.uuid,
                            any(),
                            "Hervatting: $reasonResumed"
                        )
                        zaakVariabelenService.removeDatumtijdOpgeschort(zaak.uuid)
                        zaakVariabelenService.removeVerwachteDagenOpgeschort(zaak.uuid)
                    }
                    with(patchedZaak.captured) {
                        opschorting.reden shouldBe reasonResumed
                        // einddatumGepland + 3 days since suspended - 1 day expected for suspend
                        einddatumGepland shouldBe zaak.einddatumGepland.plusDays(2)
                        // uiterlijkeEinddatumAfdoening + 3 days since suspended - 1 day expected for suspend
                        uiterlijkeEinddatumAfdoening shouldBe zaak.uiterlijkeEinddatumAfdoening.plusDays(2)
                    }
                }
            }

            When("the zaak is resumed with a resume date") {
                clearMocks(zrcClientService, zaakVariabelenService, answers = false, verificationMarks = true)

                // Response received on Friday, processing it on Monday. Resume date set to Friday (3 days ago)
                suspensionZaakHelper.resumeZaak(zaak, reasonResumed, ZonedDateTime.now().minusDays(3))

                Then("the zaak should be resumed") {
                    verify(exactly = 1) {
                        zrcClientService.patchZaak(
                            zaak.uuid,
                            any(),
                            "Hervatting: $reasonResumed"
                        )
                        zaakVariabelenService.removeDatumtijdOpgeschort(zaak.uuid)
                        zaakVariabelenService.removeVerwachteDagenOpgeschort(zaak.uuid)
                    }
                    with(patchedZaak.captured) {
                        opschorting.reden shouldBe reasonResumed
                        // einddatumGepland + 0 days since suspended - 1 day expected for suspend
                        einddatumGepland shouldBe zaak.einddatumGepland.minusDays(1)
                        // uiterlijkeEinddatumAfdoening + 0 days since suspended - 1 day expected for suspend
                        uiterlijkeEinddatumAfdoening shouldBe zaak.uiterlijkeEinddatumAfdoening.minusDays(1)
                    }
                }
            }

            When("the zaak is resumed with date before the suspension date") {
                clearMocks(zrcClientService, zaakVariabelenService, answers = false, verificationMarks = true)

                val yesterday = ZonedDateTime.now().minusDays(1)
                val dayBeforeYesterday = yesterday.minusDays(1)

                every { zaakVariabelenService.findDatumtijdOpgeschort(zaak.uuid) } returns yesterday

                val exception = shouldThrow<IllegalArgumentException> {
                    suspensionZaakHelper.resumeZaak(zaak, reasonResumed, dayBeforeYesterday)
                }

                Then("exception pointing out the reason is thrown") {
                    exception.message shouldContain yesterday.toString()
                    exception.message shouldContain dayBeforeYesterday.toString()
                }

                And("zaak is not patched") {
                    verify(exactly = 0) {
                        zrcClientService.patchZaak(zaak.uuid, any(), any())
                    }
                }
            }
        }
    }

    Context("Extend zaak") {
        Given("a zaak with a final date") {
            val extensionDescription = "extension description"
            val zaak = createZaak(
                einddatumGepland = LocalDate.now().plusDays(1),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(2)
            )
            val extendedZaak = createZaak(
                opschorting = createOpschorting(reden = null),
                einddatumGepland = LocalDate.now().plusDays(3),
                uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(4)
            )
            val patchedZaak = slot<Zaak>()

            every {
                zrcClientService.patchZaak(zaak.uuid, capture(patchedZaak), extensionDescription)
            } returns extendedZaak

            When("extension of the final date is requested from user with access") {
                every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny(
                    wijzigen = true,
                    verlengenDoorlooptijd = true
                )

                val result = suspensionZaakHelper.extendZaakFatalDate(zaak, 2, extensionDescription)

                Then("the correct dates are set") {
                    result.uiterlijkeEinddatumAfdoening shouldBe extendedZaak.uiterlijkeEinddatumAfdoening
                    result.einddatumGepland shouldBe extendedZaak.einddatumGepland
                }
            }

            When("extension of the final date is requested from user with no access") {
                every { policyService.readZaakRechten(zaak) } returns createZaakRechtenAllDeny()

                val exception = shouldThrow<PolicyException> {
                    suspensionZaakHelper.extendZaakFatalDate(zaak, 1, extensionDescription)
                }

                Then("it throws exception with no message") { exception.message shouldBe null }
            }
        }
    }
})
