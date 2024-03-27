package net.atos.zac.audit.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.zgw.shared.model.audit.zaken.objecten.ZaakobjectProductaanvraagWijziging
import net.atos.client.zgw.zrc.model.Objecttype
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import net.atos.zac.app.audit.converter.zaken.AuditZaakobjectWijzigingConverter
import java.net.URI

class AuditZaakobjectWijzigingConverterTest : BehaviorSpec({
    val converter = AuditZaakobjectWijzigingConverter()
    Given("A object creation for type Overig") {
        val wijzigingen = ZaakobjectProductaanvraagWijziging()
        val aanvraag = ZaakobjectProductaanvraag()
        wijzigingen.nieuw = aanvraag
        aanvraag.objectType = Objecttype.OVERIGE
        aanvraag.`object` = URI("www.google.nl/12345")
        When("The wijziging is converted") {
            val regels = converter.convert(wijzigingen).toList()
            Then("It should not show up in the list") {
                with(regels) {
                    size shouldBe 0
                }
            }
        }
    }
})
