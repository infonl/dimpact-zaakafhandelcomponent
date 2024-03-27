package net.atos.zac.audit.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.zgw.shared.model.audit.zaken.objecten.ZaakobjectProductaanvraagWijziging
import net.atos.client.zgw.shared.model.audit.zaken.objecten.ZaakobjectWoonplaatsWijziging
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.zaakobjecten.ObjectWoonplaats
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import net.atos.zac.app.audit.converter.zaken.AuditZaakobjectWijzigingConverter
import java.net.URI

class AuditZaakobjectWijzigingConverterTest : BehaviorSpec({
    val converter = AuditZaakobjectWijzigingConverter()

    Given("A ZaakobjectProductaanvraagWijziging") {
        val wijzigingen = ZaakobjectProductaanvraagWijziging()
        val aanvraag = ZaakobjectProductaanvraag()
        wijzigingen.nieuw = aanvraag
        aanvraag.objectType = Objecttype.OVERIGE
        aanvraag.`object` = URI("http://example.com/12345")

        When("The wijziging is converted") {
            val regels = converter.convert(wijzigingen).toList()

            Then("It should not show up in the list") {
                with(regels) {
                    size shouldBe 0
                }
            }
        }
    }

    Given("A ZaakobjectWoonplaatsWijziging with oud and nieuw") {
        val wijzigingen = ZaakobjectWoonplaatsWijziging()
        val zaakobjectWoonplaatsOud =
            ZaakobjectWoonplaats(
                URI("http://example.com/12345"),
                URI("http://example.com/12345"),
                ObjectWoonplaats("identificatie-oud", "woonplaats-oud")
            )
        val zaakobjectWoonplaatsNieuw =
            ZaakobjectWoonplaats(
                URI("http://example.com/54321"),
                URI("http://example.com/54321"),
                ObjectWoonplaats("identificatie-nieuw", "woonplaats-nieuw")
            )
        wijzigingen.oud = zaakobjectWoonplaatsOud
        wijzigingen.nieuw = zaakobjectWoonplaatsNieuw

        When("The wijziging is converted") {
            val regels = converter.convert(wijzigingen).toList()
            Then("It should show up in the list") {
                with(regels) {
                    size shouldBe 1
                }
            }
            Then("The expected values are present") {
                with(regels.first()) {
                    oudeWaarde shouldBe "identificatie-oud"
                    nieuweWaarde shouldBe "identificatie-nieuw"
                    attribuutLabel shouldBe "objecttype.WOONPLAATS"
                }
            }
        }
    }

    Given("A ZaakobjectWoonplaatsWijziging with only nieuw") {
        val wijzigingen = ZaakobjectWoonplaatsWijziging()
        val zaakobjectWoonplaatsNieuw =
            ZaakobjectWoonplaats(
                URI("http://example.com/54321"),
                URI("http://example.com/54321"),
                ObjectWoonplaats("identificatie-nieuw", "woonplaats-nieuw")
            )
        wijzigingen.nieuw = zaakobjectWoonplaatsNieuw

        When("The wijziging is converted") {
            val regels = converter.convert(wijzigingen).toList()
            Then("It should show up in the list") {
                with(regels) {
                    size shouldBe 1
                }
            }
            Then("The expected values are present") {
                with(regels.first()) {
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "identificatie-nieuw"
                    attribuutLabel shouldBe "objecttype.WOONPLAATS"
                }
            }
        }
    }
})
