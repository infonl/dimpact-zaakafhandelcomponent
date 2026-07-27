/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.model.document

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.model.createZaakobjectProductaanvraag
import nl.info.zac.productaanvraag.model.createProductaanvraagDimpact

class SmartDocumentsDocumentRequestDeclarationsTest : BehaviorSpec({
    given("A zaakobject of type productaanvraag and its resolved product aanvraag and aanvraaggegevens") {
        val zaakobjectProductaanvraag = createZaakobjectProductaanvraag()
        val productaanvraagDimpact = createProductaanvraagDimpact(type = "fakeProductAanvraagType")
        val aanvraaggegevens = mapOf("fakeKey" to "fakeValue")

        `when`("it is converted to startformulier data") {
            val startformulierData = zaakobjectProductaanvraag.toStartformulierData(
                productaanvraag = productaanvraagDimpact,
                aanvraaggegevens = aanvraaggegevens
            )

            then("the startformulier data is derived from the product aanvraag and aanvraaggegevens") {
                startformulierData.productAanvraagtype shouldBe productaanvraagDimpact.type
                startformulierData.data shouldBe aanvraaggegevens
            }
        }
    }
})
