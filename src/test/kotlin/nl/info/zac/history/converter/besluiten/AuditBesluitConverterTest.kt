/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.besluiten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.shared.model.audit.besluiten.BesluitWijziging
import java.time.LocalDate

class AuditBesluitConverterTest : BehaviorSpec({

    given("a besluit wijziging without an old besluit") {
        val besluitWijziging = BesluitWijziging().apply {
            nieuw = createBesluit(identificatie = "fakeNieuweIdentificatie")
        }

        `when`("it is converted to history lines") {
            val historyLines = besluitWijziging.toHistoryLines()

            then("it returns one line with the identificatie of the new besluit") {
                historyLines.map { Triple(it.attributeLabel, it.oldValue, it.newValue) } shouldContainExactly listOf(
                    Triple("Besluit", null, "fakeNieuweIdentificatie")
                )
            }
        }
    }

    given("a besluit wijziging without a new besluit") {
        val besluitWijziging = BesluitWijziging().apply {
            oud = createBesluit(identificatie = "fakeOudeIdentificatie")
        }

        `when`("it is converted to history lines") {
            val historyLines = besluitWijziging.toHistoryLines()

            then("it returns one line with the identificatie of the old besluit") {
                historyLines.map { Triple(it.attributeLabel, it.oldValue, it.newValue) } shouldContainExactly listOf(
                    Triple("Besluit", "fakeOudeIdentificatie", null)
                )
            }
        }
    }

    given("a besluit wijziging in which all tracked attributes changed") {
        val besluitWijziging = BesluitWijziging().apply {
            oud = createBesluit(
                identificatie = "fakeOudeIdentificatie",
                reason = "fakeOudeToelichting",
                startDate = LocalDate.of(2024, 1, 1),
                fatalDate = LocalDate.of(2024, 2, 1)
            ).apply { verzenddatum = LocalDate.of(2024, 3, 1) }
            nieuw = createBesluit(
                identificatie = "fakeNieuweIdentificatie",
                reason = "fakeNieuweToelichting",
                startDate = LocalDate.of(2025, 1, 1),
                fatalDate = LocalDate.of(2025, 2, 1)
            ).apply { verzenddatum = LocalDate.of(2025, 3, 1) }
        }

        `when`("it is converted to history lines") {
            val historyLines = besluitWijziging.toHistoryLines()

            then("it returns one line per changed attribute with the old and the new value") {
                historyLines.map { Triple(it.attributeLabel, it.oldValue, it.newValue) } shouldContainExactly listOf(
                    Triple("identificatie", "fakeOudeIdentificatie", "fakeNieuweIdentificatie"),
                    Triple("verzenddatum", "01-03-2024", "01-03-2025"),
                    Triple("ingangsdatum", "01-01-2024", "01-01-2025"),
                    Triple("vervaldatum", "01-02-2024", "01-02-2025"),
                    Triple("toelichting", "fakeOudeToelichting", "fakeNieuweToelichting")
                )
            }
        }
    }

    given("a besluit wijziging in which none of the tracked attributes changed") {
        val besluit = createBesluit(startDate = LocalDate.of(2024, 1, 1), fatalDate = LocalDate.of(2024, 2, 1))
        val besluitWijziging = BesluitWijziging().apply {
            oud = besluit
            nieuw = besluit
        }

        `when`("it is converted to history lines") {
            val historyLines = besluitWijziging.toHistoryLines()

            then("it returns no lines") {
                historyLines.shouldBeEmpty()
            }
        }
    }
})
