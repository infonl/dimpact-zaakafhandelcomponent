package net.atos.zac.shared.helper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createOpschorting
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.exception.PolicyException
import net.atos.zac.policy.output.createZaakRechten
import java.time.LocalDate
import java.util.Optional

class OpschortenZaakHelperTest : BehaviorSpec({
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZRCClientService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()

    val opschortenZaakHelper = OpschortenZaakHelper(
        policyService,
        zrcClientService,
        zaakVariabelenService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("a zaak that is open and not yet postponed and does not have an planned end date") {
        val numberOfDaysPostponed = 123L
        val postPonementReason = "dummyReason"
        val zaak = createZaak(
            opschorting = createOpschorting(reden = null),
            einddatumGepland = null,
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1)
        )
        val postponedZaak = createZaak(
            opschorting = createOpschorting(reden = "dummyReason"),
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
            every { policyService.readZaakRechten(zaak) } returns createZaakRechten(opschorten = true)

            val returnedZaak = opschortenZaakHelper.opschortenZaak(
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
            every { policyService.readZaakRechten(zaak) } returns createZaakRechten()

            val exception = shouldThrow<PolicyException> {
                opschortenZaakHelper.opschortenZaak(
                    zaak,
                    numberOfDaysPostponed,
                    postPonementReason
                )
            }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }
    Given("a zaak that is postponed and does not have an planned end date") {
        val reasonResumed = "dummyResumeReason"
        val zaak = createZaak(
            opschorting = createOpschorting(
                reden = "dummyPostponementReason",
                indicatie = true
            ),
            einddatumGepland = null,
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1)
        )
        val resumedZaak = createZaak(
            opschorting = createOpschorting(reden = null),
            einddatumGepland = null,
            uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1)
        )
        val patchedZaak = slot<Zaak>()

        every { zaakVariabelenService.findDatumtijdOpgeschort(zaak.uuid) } returns Optional.empty()
        every { zaakVariabelenService.findVerwachteDagenOpgeschort(zaak.uuid) } returns Optional.empty()
        every {
            zrcClientService.patchZaak(
                zaak.uuid,
                capture(patchedZaak),
                "Hervatting: $reasonResumed"
            )
        } returns resumedZaak
        every { zaakVariabelenService.removeDatumtijdOpgeschort(zaak.uuid) } just runs
        every { zaakVariabelenService.removeVerwachteDagenOpgeschort(zaak.uuid) } just runs

        When("the zaak is resumed from user with access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechten(hervatten = true)

            opschortenZaakHelper.hervattenZaak(zaak, reasonResumed)

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
                    uiterlijkeEinddatumAfdoening shouldBe resumedZaak.uiterlijkeEinddatumAfdoening
                }
            }
        }

        When("the zaak is resumed from user with no access") {
            every { policyService.readZaakRechten(zaak) } returns createZaakRechten()

            val exception = shouldThrow<PolicyException> { opschortenZaakHelper.hervattenZaak(zaak, reasonResumed) }

            Then("it throws exception with no message") { exception.message shouldBe null }
        }
    }
})
