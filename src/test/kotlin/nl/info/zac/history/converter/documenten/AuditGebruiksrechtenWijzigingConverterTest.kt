/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.documenten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import nl.info.client.zgw.drc.model.createGebruiksrechten
import nl.info.client.zgw.shared.model.audit.documenten.GebruiksrechtenWijziging

class AuditGebruiksrechtenWijzigingConverterTest : BehaviorSpec({

    given("a gebruiksrechten wijziging with old and new gebruiksrechten") {
        val gebruiksrechtenWijziging = GebruiksrechtenWijziging().apply {
            oud = createGebruiksrechten(omschrijvingVoorwaarden = "fakeOudeVoorwaarden")
            nieuw = createGebruiksrechten(omschrijvingVoorwaarden = "fakeNieuweVoorwaarden")
        }

        `when`("it is converted to history lines") {
            val historyLines = gebruiksrechtenWijziging.toHistoryLines()

            then("it returns one line with the old and the new omschrijving of the voorwaarden") {
                historyLines.map { Triple(it.attributeLabel, it.oldValue, it.newValue) } shouldContainExactly listOf(
                    Triple("indicatieGebruiksrecht", "fakeOudeVoorwaarden", "fakeNieuweVoorwaarden")
                )
            }
        }
    }

    given("a gebruiksrechten wijziging without old gebruiksrechten") {
        val gebruiksrechtenWijziging = GebruiksrechtenWijziging().apply {
            nieuw = createGebruiksrechten(omschrijvingVoorwaarden = "fakeNieuweVoorwaarden")
        }

        `when`("it is converted to history lines") {
            val historyLines = gebruiksrechtenWijziging.toHistoryLines()

            then("it returns one line without an old value") {
                historyLines.map { Triple(it.attributeLabel, it.oldValue, it.newValue) } shouldContainExactly listOf(
                    Triple("indicatieGebruiksrecht", null, "fakeNieuweVoorwaarden")
                )
            }
        }
    }
})
