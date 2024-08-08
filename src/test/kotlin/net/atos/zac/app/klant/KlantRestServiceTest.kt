package net.atos.zac.app.klant

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.BrpClientService
import net.atos.client.klant.KlantClientService
import net.atos.client.klant.createDigitalAddresses
import net.atos.client.kvk.KvkClientService
import net.atos.client.kvk.zoeken.model.createAdresWithBinnenlandsAdres
import net.atos.client.kvk.zoeken.model.createResultaatItem
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.klant.converter.KlantcontactConverter
import net.atos.zac.app.klant.converter.RestPersoonConverter
import net.atos.zac.app.klant.converter.RestVestigingsprofielConverter
import java.util.Optional
import java.util.concurrent.CompletableFuture

const val NON_BREAKING_SPACE = '\u00A0'.toString()

class KlantRestServiceTest : BehaviorSpec({
    val brpClientService = mockk<BrpClientService>()
    val kvkClientService = mockk<KvkClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val restPersoonConverter = mockk<RestPersoonConverter>()
    val restVestigingsprofielConverter = mockk<RestVestigingsprofielConverter>()
    val klantClientService = mockk<KlantClientService>()
    val klantcontactConverter = KlantcontactConverter()
    val klantRestService = KlantRestService(
        brpClientService,
        kvkClientService,
        ztcClientService,
        restPersoonConverter,
        restVestigingsprofielConverter,
        klantClientService,
        klantcontactConverter
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
        val digitalAddressesList = createDigitalAddresses("+123-456-789", "email@server.xyz")
        every {
            kvkClientService.findVestigingAsync(vestigingsnummer)
        } returns CompletableFuture.completedFuture(Optional.of(kvkResultaatItem))
        every {
            klantClientService.findDigitalAddressesByNumber(vestigingsnummer)
        } returns digitalAddressesList

        When("a request is made to get all klanten") {
            val restBedrijf = klantRestService.readVestiging(vestigingsnummer)

            Then("it should return all klanten") {
                with(restBedrijf) {
                    this.adres shouldBe with(adres.binnenlandsAdres) {
                        "$straatnaam$NON_BREAKING_SPACE$huisnummer$NON_BREAKING_SPACE$huisletter, $postcode, $plaats"
                    }
                    emailadres shouldBe "email@server.xyz"
                    naam shouldBe kvkResultaatItem.naam
                    kvkNummer shouldBe kvkResultaatItem.kvkNummer
                    postcode shouldBe kvkResultaatItem.adres.binnenlandsAdres.postcode
                    rsin shouldBe kvkResultaatItem.rsin
                    type shouldBe "NEVENVESTIGING"
                    telefoonnummer shouldBe "+123-456-789"
                    this.vestigingsnummer shouldBe vestigingsnummer
                }
            }
        }
    }
})
