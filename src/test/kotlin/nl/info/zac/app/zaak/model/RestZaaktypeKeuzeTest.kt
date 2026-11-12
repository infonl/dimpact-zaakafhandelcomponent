/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.createZaakType

class RestZaaktypeKeuzeTest : BehaviorSpec({
    given("A zaaktype") {
        val zaaktype = createZaakType(omschrijving = "fakeOmschrijving")

        `when`("it is converted to a zaaktype choice") {
            val restZaaktypeKeuze = zaaktype.toRestZaaktypeKeuze()

            then("the choice has only the uuid and the omschrijving of the zaaktype") {
                restZaaktypeKeuze shouldBe RestZaaktypeKeuze(
                    uuid = zaaktype.url.extractUuid(),
                    omschrijving = "fakeOmschrijving"
                )
            }
        }
    }
})
