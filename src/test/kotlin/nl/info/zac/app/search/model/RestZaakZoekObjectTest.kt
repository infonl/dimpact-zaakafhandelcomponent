/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.policy.output.createZaakRechten
import nl.info.zac.search.model.createZaakZoekObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

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

    given("a zaak zoek object with a different date in each date field") {
        fun LocalDate.toDate() = Date.from(atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
        val archiefActiedatum = LocalDate.of(2030, 1, 1)
        val registratiedatum = LocalDate.of(2026, 3, 10)
        val startdatum = LocalDate.of(2026, 2, 1)
        val einddatum = LocalDate.of(2026, 5, 15)
        val einddatumGepland = LocalDate.of(2026, 4, 30)
        val uiterlijkeEinddatumAfdoening = LocalDate.of(2026, 6, 1)
        val publicatiedatum = LocalDate.of(2026, 3, 20)
        val zaakZoekObject = createZaakZoekObject().apply {
            this.archiefActiedatum = archiefActiedatum.toDate()
            this.registratiedatum = registratiedatum.toDate()
            this.startdatum = startdatum.toDate()
            this.einddatum = einddatum.toDate()
            this.einddatumGepland = einddatumGepland.toDate()
            this.uiterlijkeEinddatumAfdoening = uiterlijkeEinddatumAfdoening.toDate()
            this.publicatiedatum = publicatiedatum.toDate()
        }

        `when`("it is converted to its REST representation") {
            val restZaakZoekObject = zaakZoekObject.toRestZaakZoekObject(createZaakRechten())

            then("each date is converted from its own date field") {
                with(restZaakZoekObject) {
                    this.archiefActiedatum shouldBe archiefActiedatum
                    this.registratiedatum shouldBe registratiedatum
                    this.startdatum shouldBe startdatum
                    this.einddatum shouldBe einddatum
                    this.einddatumGepland shouldBe einddatumGepland
                    this.uiterlijkeEinddatumAfdoening shouldBe uiterlijkeEinddatumAfdoening
                    this.publicatiedatum shouldBe publicatiedatum
                }
            }
        }
    }
})
