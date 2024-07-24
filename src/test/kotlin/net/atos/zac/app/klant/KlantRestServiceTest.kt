package net.atos.zac.app.klant

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.BRPClientService
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.createKlant
import net.atos.client.klant.model.Klant
import net.atos.client.kvk.KvkClientService
import net.atos.client.kvk.zoeken.model.createAdresWithBinnenlandsAdres
import net.atos.client.kvk.zoeken.model.createResultaatItem
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.klant.converter.RestPersoonConverter
import net.atos.zac.app.klant.converter.RestVestigingsprofielConverter
import java.util.Optional
import java.util.concurrent.CompletableFuture

const val NON_BREAKING_SPACE = '\u00A0'.toString()

class KlantRestServiceTest : BehaviorSpec({
    val brpClientService = mockk<BRPClientService>()
    val kvkClientService = mockk<KvkClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val restPersoonConverter = mockk<RestPersoonConverter>()
    val restVestigingsprofielConverter = mockk<RestVestigingsprofielConverter>()
    val klantClientService = mockk<KlantClientService>()
    val klantRestService = KlantRestService(
        brpClientService,
        kvkClientService,
        ztcClientService,
        restPersoonConverter,
        restVestigingsprofielConverter,
        klantClientService
    )

    Given(
        """
        a vestiging for which a company exists in the KVK client and for which a customer exists in the klanten client
        """
    ) {
        val vestigingsnummer = "dummyVestigingsnummer"
        val adres = createAdresWithBinnenlandsAdres()
        val kvkResultaatItem = createResultaatItem(
            adres = adres,
            type = "nevenvestiging",
            vestingsnummer = vestigingsnummer
        )
        val klant = createKlant(
            subjectType = Klant.SubjectTypeEnum.VESTIGING
        )
        every {
            kvkClientService.findVestigingAsync(vestigingsnummer)
        } returns CompletableFuture.completedFuture(Optional.of(kvkResultaatItem))
        every {
            klantClientService.findVestigingAsync(vestigingsnummer)
        } returns CompletableFuture.completedFuture(Optional.of(klant))

        When("a request is made to get all klanten") {
            val restBedrijf = klantRestService.readVestiging(vestigingsnummer)

            Then("it should return all klanten") {
                with(restBedrijf) {
                    this.adres shouldBe with(adres.binnenlandsAdres) {
                        "$straatnaam$NON_BREAKING_SPACE$huisnummer$NON_BREAKING_SPACE$huisletter, $postcode, $plaats"
                    }
                    emailadres shouldBe klant.emailadres
                    naam shouldBe kvkResultaatItem.naam
                    kvkNummer shouldBe kvkResultaatItem.kvkNummer
                    postcode shouldBe kvkResultaatItem.adres.binnenlandsAdres.postcode
                    rsin shouldBe kvkResultaatItem.rsin
                    type shouldBe "NEVENVESTIGING"
                    telefoonnummer shouldBe klant.telefoonnummer
                    this.vestigingsnummer shouldBe vestigingsnummer
                }
            }
        }
    }
})
