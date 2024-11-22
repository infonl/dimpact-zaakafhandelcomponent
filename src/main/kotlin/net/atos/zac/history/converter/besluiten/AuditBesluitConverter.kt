/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.history.converter.besluiten

import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.history.converter.addHistorieRegel
import net.atos.zac.history.model.HistoryLine

object AuditBesluitConverter {

    fun convert(wijziging: AuditWijziging<Besluit>): List<HistoryLine> {
        val oud = wijziging.oud
        val nieuw = wijziging.nieuw

        if (oud == null || nieuw == null) {
            return listOf(HistoryLine("Besluit", toWaarde(oud), toWaarde(nieuw)))
        }

        return mutableListOf<HistoryLine>().apply {
            addHistorieRegel("identificatie", oud.identificatie, nieuw.identificatie)
            addHistorieRegel("verzenddatum", oud.verzenddatum, nieuw.verzenddatum)
            addHistorieRegel("ingangsdatum", oud.ingangsdatum, nieuw.ingangsdatum)
            addHistorieRegel("vervaldatum", oud.vervaldatum, nieuw.vervaldatum)
            addHistorieRegel("toelichting", oud.toelichting, nieuw.toelichting)
        }
    }

    private fun toWaarde(besluit: Besluit?): String? =
        besluit?.identificatie
}
