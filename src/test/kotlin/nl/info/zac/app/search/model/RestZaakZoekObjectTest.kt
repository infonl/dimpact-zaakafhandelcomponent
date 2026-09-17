/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.createZaakZoekObject

class RestZaakZoekObjectTest : BehaviorSpec({
    given("a zaak zoek object of a zaakspecifiek geautoriseerde zaak") {
        val zaakZoekObject = createZaakZoekObject(isZaakspecifiekGeautoriseerd = true)

        `when`("it is converted to its REST representation") {
            val restZaakZoekObject = zaakZoekObject.toRestZaakZoekObject(createZaakRechten())

            then("the werklijst row reports the zaak as zaakspecifiek geautoriseerd") {
                restZaakZoekObject.isZaakspecifiekGeautoriseerd shouldBe true
            }
        }
    }

    given("a zaak zoek object of a zaak that is not zaakspecifiek geautoriseerd") {
        val zaakZoekObject = createZaakZoekObject(isZaakspecifiekGeautoriseerd = false)

        `when`("it is converted to its REST representation") {
            val restZaakZoekObject = zaakZoekObject.toRestZaakZoekObject(createZaakRechten())

            then("the werklijst row reports the zaak as not zaakspecifiek geautoriseerd") {
                restZaakZoekObject.isZaakspecifiekGeautoriseerd shouldBe false
            }
        }
    }
})
