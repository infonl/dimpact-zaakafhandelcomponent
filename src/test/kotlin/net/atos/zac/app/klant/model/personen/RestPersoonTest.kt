package net.atos.zac.app.klant.model.personen

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.brp.model.createPersoon
import net.atos.client.brp.model.createPersoonBeperkt
import net.atos.client.brp.model.generated.AbstractDatum
import net.atos.client.brp.model.generated.Adressering
import net.atos.client.brp.model.generated.AdresseringBeperkt
import net.atos.client.brp.model.generated.OpschortingBijhouding
import net.atos.client.brp.model.generated.PersoonInOnderzoek
import net.atos.client.brp.model.generated.PersoonInOnderzoekBeperkt
import net.atos.client.brp.model.generated.RniDeelnemer
import net.atos.client.brp.model.generated.Waardetabel
import java.util.EnumSet

class RestPersoonTest : BehaviorSpec({

    Given("BRP Persoon with all flags") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoon = createPersoon(
            confidentialPersonalData = true,
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "O"
                    omschrijving = "overlijden"
                }
                datum = date
            },
            indicationCuratoriesRegister = true,
            personInResearch = PersoonInOnderzoek(),
            rniDeelnemerList = listOf(RniDeelnemer()),
            address = Adressering().apply { indicatieVastgesteldVerblijftNietOpAdres = true }
        )

        When("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            Then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.allOf(RestPersoonIndicaties::class.java)
            }
        }
    }

    Given("BRP PersoonBeperkt with all flags") {
        val date = AbstractDatum().apply {
            type = "type"
            langFormaat = "langFormaat"
        }
        val persoonBeperkt = createPersoonBeperkt(
            confidentialPersonalData = true,
            suspensionMaintenance = OpschortingBijhouding().apply {
                reden = Waardetabel().apply {
                    code = "O"
                    omschrijving = "overlijden"
                }
                datum = date
            },
            personInResearch = PersoonInOnderzoekBeperkt(),
            rniDeelnemerList = listOf(RniDeelnemer()),
            address = AdresseringBeperkt().apply { indicatieVastgesteldVerblijftNietOpAdres = true }
        )

        When("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            Then("conversion is correct") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                // check for all but ONDER_CURATELE
                restPersoon.indicaties shouldBe EnumSet.complementOf(EnumSet.of(RestPersoonIndicaties.ONDER_CURATELE))
            }
        }
    }

    Given("BRP Persoon that's in research and has confidential personal data") {
        val persoon = createPersoon(
            confidentialPersonalData = true,
            personInResearch = PersoonInOnderzoek(),
        )

        When("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            Then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.IN_ONDERZOEK,
                    RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS
                )
            }
        }
    }

    Given("BRP PersoonBeperkt that's in research and has confidential personal data") {
        val persoonBeperkt = createPersoonBeperkt(
            confidentialPersonalData = true,
            personInResearch = PersoonInOnderzoekBeperkt(),
        )

        When("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            Then("conversion is correct") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.of(
                    RestPersoonIndicaties.IN_ONDERZOEK,
                    RestPersoonIndicaties.GEHEIMHOUDING_OP_PERSOONSGEGEVENS
                )
            }
        }
    }

    Given("BRP Persoon that has no indication relative data") {
        val persoon = createPersoon()

        When("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            Then("conversion yields no indications") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.noneOf(RestPersoonIndicaties::class.java)
            }
        }
    }

    Given("BRP PersoonBeperkt that has no indication relative data") {
        val persoonBeperkt = createPersoonBeperkt()

        When("converted to RestPersoon") {
            val restPersoon = persoonBeperkt.toRestPersoon()

            Then("conversion yields no indications") {
                restPersoon.bsn shouldBe persoonBeperkt.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.noneOf(RestPersoonIndicaties::class.java)
            }
        }
    }
})
