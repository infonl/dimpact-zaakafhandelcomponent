package net.atos.zac.aanvraag

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.or.`object`.model.createORObject
import net.atos.client.or.`object`.model.createObjectRecord
import net.atos.zac.aanvraag.model.generated.Betrokkene

class ProductaanvraagServiceTest : BehaviorSpec({
    val productaanvraagService = ProductaanvraagService()

    Given("an object registration object") {
        val bron = createBron()
        val betrokkenen = listOf(
            createBetrokkene(
                inBsn = "dummyBsn",
                rolOmschrijvingGeneriek = Betrokkene.RolOmschrijvingGeneriek.INITIATOR
            )
        )
        val orObject = createORObject(
            record = createObjectRecord(
                data = mapOf(
                    "bron" to bron,
                    "type" to "productaanvraag",
                    "betrokkenen" to betrokkenen
                )
            )
        )
        When("the productaanvraag is requested from the product aanvraag service") {
            val productAanVraagDimpact = productaanvraagService.getProductaanvraag(orObject)

            Then("the productaanvraag of type 'productaanvraag Dimpact' is returned and contains the expected data") {
                with(productAanVraagDimpact) {
                    with(this.bron) {
                        naam shouldBe bron.naam
                        kenmerk shouldBe bron.kenmerk
                    }
                    type shouldBe "productaanvraag"
                    with(betrokkenen[0]) {
                        inpBsn shouldBe betrokkenen[0].inpBsn
                        rolOmschrijvingGeneriek shouldBe betrokkenen[0].rolOmschrijvingGeneriek
                    }
                }
            }
        }
    }
})
