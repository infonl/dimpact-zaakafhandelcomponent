package net.atos.zac.shared.helper

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.ints.exactly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createOpschorting
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createZaakRechten
import java.time.LocalDate
import java.util.Optional

class OpschortenZaakHelperTest : BehaviorSpec() {
    val policyService = mockk<PolicyService>()
    val zrcClientService = mockk<ZRCClientService>()
    val ztcClientService = mockk<ZTCClientService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()

    // We have to use @InjectMockKs since the class under test uses field injection instead of constructor injection.
    // This is because WildFly does not properly support constructor injection.
    @InjectMockKs
    lateinit var opschortenZaakHelper: OpschortenZaakHelper

    override suspend fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
    }

    init {
        given("a zaak that is open and not yet postponed and does not have an planned end date") {
            When("the zaak is postponed for x days") {
                then("the zaak should be postponed and the final date should be extended with x days") {
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
                        uiterlijkeEinddatumAfdoening = LocalDate.now().plusDays(1 + numberOfDaysPostponed)
                    )
                    val patchedZaak = slot<Zaak>()

                    every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
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

                    val returnedZaak = opschortenZaakHelper.opschortenZaak(
                        zaak,
                        numberOfDaysPostponed,
                        postPonementReason
                    )

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
        }
        given("a zaak that is postponed and does not have an planned end date") {
            When("the zaak is resumed") {
                then("the zaak should be resumed") {
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

                    every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
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

                    opschortenZaakHelper.hervattenZaak(zaak, reasonResumed)

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
        }
    }
}
