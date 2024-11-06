package net.atos.zac.app.klant.model.personen

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.brp.model.generated.AbstractDatum
import net.atos.client.brp.model.generated.OpschortingBijhouding
import net.atos.client.brp.model.generated.PersoonInOnderzoek
import net.atos.client.brp.model.generated.RniDeelnemer
import net.atos.client.brp.model.generated.Waardetabel
import java.util.EnumSet

class RestPersoonTest : BehaviorSpec({

    Given("Dead BRP Persoon") {
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
        )

        When("converted to RestPersoon") {
            val restPersoon = persoon.toRestPersoon()

            Then("conversion is correct") {
                restPersoon.bsn shouldBe persoon.burgerservicenummer
                restPersoon.indicaties shouldBe EnumSet.allOf(RestPersoonIndicaties::class.java)
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
})
