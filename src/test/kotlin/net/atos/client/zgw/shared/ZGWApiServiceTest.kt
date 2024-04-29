package net.atos.client.zgw.shared

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.exactly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.createResultaat
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.generated.Resultaat
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createResultaatType
import java.net.URI
import java.util.UUID

class ZGWApiServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZTCClientService>()
    val zrcClientService = mockk<ZRCClientService>()
    val drcClientService = mockk<DRCClientService>()
    val zgwApiService = ZGWApiService(
        ztcClientService,
        zrcClientService,
        drcClientService
    )
    val resultaatTypeUUID = UUID.randomUUID()
    val reason = "dummyReason"

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("A zaak with an existing result") {
        val dummyResultaat = URI("http://example.com/${UUID.randomUUID()}")
        val zaak = createZaak(
            resultaat = dummyResultaat
        )
        val resultaat = createResultaat()
        val resultaatSlot = slot<Resultaat>()
        val updatedResultaat = createResultaat()
        val resultaattType = createResultaatType()
        every { zrcClientService.readResultaat(zaak.resultaat) } returns resultaat
        every { zrcClientService.deleteResultaat(resultaat.uuid) } just Runs
        every { ztcClientService.readResultaattype(resultaatTypeUUID) } returns resultaattType
        every { zrcClientService.createResultaat(capture(resultaatSlot)) } returns updatedResultaat

        When("when the result is updated for the zaak") {
            zgwApiService.updateResultaatForZaak(
                zaak,
                resultaatTypeUUID,
                reason
            )

            Then("the existing zaak result should be updated") {
                verify(exactly = 1) {
                    zrcClientService.readResultaat(zaak.resultaat)
                    zrcClientService.deleteResultaat(resultaat.uuid)
                    ztcClientService.readResultaattype(resultaatTypeUUID)
                    zrcClientService.createResultaat(any())
                }
                resultaatSlot.captured.run {
                    this.uuid shouldBe null
                    this.zaak shouldBe zaak.url
                    this.resultaattype shouldBe resultaattType.url
                    this.toelichting shouldBe reason
                }
            }
        }
    }
    Given("A zaak without an existing result") {
        clearAllMocks()
        val zaak = createZaak(
            resultaat = null
        )
        val resultaatSlot = slot<Resultaat>()
        val updatedResultaat = createResultaat()
        val resultaattType = createResultaatType()
        every { ztcClientService.readResultaattype(resultaatTypeUUID) } returns resultaattType
        every { zrcClientService.createResultaat(capture(resultaatSlot)) } returns updatedResultaat

        When("when the result is updated for the zaak") {
            zgwApiService.updateResultaatForZaak(
                zaak,
                resultaatTypeUUID,
                reason
            )

            Then("the zaak result should be created") {
                verify(exactly = 1) {
                    ztcClientService.readResultaattype(resultaatTypeUUID)
                    zrcClientService.createResultaat(any())
                }
                resultaatSlot.captured.run {
                    this.uuid shouldBe null
                    this.zaak shouldBe zaak.url
                    this.resultaattype shouldBe resultaattType.url
                    this.toelichting shouldBe reason
                }
            }
        }
    }
})
