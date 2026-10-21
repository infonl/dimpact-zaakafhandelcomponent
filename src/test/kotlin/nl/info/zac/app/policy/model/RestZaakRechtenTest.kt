/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.policy.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.policy.output.createZaakRechten

class RestZaakRechtenTest : BehaviorSpec({
    context("Converting zaak rechten to REST zaak rechten") {
        given("zaak rechten that allow linking zaken") {
            val zaakRechten = createZaakRechten(koppelen = true)

            `when`("they are converted") {
                val restZaakRechten = zaakRechten.toRestZaakRechten()

                then("the REST zaak rechten allow linking zaken") {
                    restZaakRechten.canKoppelen shouldBe true
                }
            }
        }

        given("zaak rechten that do not allow linking zaken") {
            val zaakRechten = createZaakRechten(koppelen = false)

            `when`("they are converted") {
                val restZaakRechten = zaakRechten.toRestZaakRechten()

                then("the REST zaak rechten do not allow linking zaken") {
                    restZaakRechten.canKoppelen shouldBe false
                }
            }
        }
    }
})
