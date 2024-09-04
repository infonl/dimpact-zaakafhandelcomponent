package net.atos.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createBesluitType
import net.atos.zac.app.informatieobjecten.converter.RestInformatieobjectConverter
import net.atos.zac.app.zaak.model.createRESTBesluitVastleggenGegevens
import java.time.LocalDate

class RestBesluitConverterTest : BehaviorSpec({
    val brcClientService = mockk<BrcClientService>()
    val drcClientService = mockk<DrcClientService>()
    val restInformatieobjectConverter = mockk<RestInformatieobjectConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val restBesluitConverter = RestBesluitConverter(
        brcClientService,
        drcClientService,
        restInformatieobjectConverter,
        ztcClientService
    )

    Given("Besluit toevoegen data with a vervaldatum") {
        val zaak = createZaak()
        val besluitToevoegenGegevens = createRESTBesluitVastleggenGegevens(
            ingangsdatum = LocalDate.now().plusDays(1),
            vervaldatum = LocalDate.now().plusDays(2)
        )
        val besluittype = createBesluitType()

        every { ztcClientService.readBesluittype(besluitToevoegenGegevens.besluittypeUuid) } returns besluittype

        When("this data is converted to a besluit") {
            val dateNow = LocalDate.now()
            val besluit = restBesluitConverter.convertToBesluit(zaak, besluitToevoegenGegevens)

            Then("the besluit is correctly converted and should have a vervalreden of type 'tijdelijk'") {
                with(besluit) {
                    this.zaak shouldBe zaak.url
                    this.besluittype shouldBe besluittype.url
                    datum shouldBe dateNow
                    ingangsdatum shouldBe besluitToevoegenGegevens.ingangsdatum
                    toelichting shouldBe besluitToevoegenGegevens.toelichting
                    vervaldatum shouldBe besluitToevoegenGegevens.vervaldatum
                    vervalreden shouldBe VervalredenEnum.TIJDELIJK
                }
            }
        }
    }
})
