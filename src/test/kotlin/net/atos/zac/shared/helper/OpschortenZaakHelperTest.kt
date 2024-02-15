package net.atos.zac.shared.helper

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.createOpschorting
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import net.atos.zac.policy.output.createZaakRechten
import java.time.LocalDate

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
                        uiterlijkeEinddatumAfdoening = LocalDate.now()
                            .plusDays(1 + numberOfDaysPostponed)
                    )

                    val patchedZaak = slot<Zaak>()
                    every { policyService.readZaakRechten(zaak) } returns createZaakRechten()
                    every {
                        zrcClientService.patchZaak(
                            zaak.uuid,
                            capture(patchedZaak),
                            "Opschorting: $postPonementReason"
                        )
                    } returns postponedZaak
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

                    val returnedZaak = opschortenZaakHelper.opschortenZaak(
                        zaak,
                        numberOfDaysPostponed,
                        postPonementReason
                    )

                    returnedZaak shouldBe postponedZaak
                    with(patchedZaak.captured) {
                        opschorting.reden shouldBe postPonementReason
                        einddatumGepland shouldBe null
                        uiterlijkeEinddatumAfdoening shouldBe postponedZaak.uiterlijkeEinddatumAfdoening
                    }
                }
            }
        }
    }
}
