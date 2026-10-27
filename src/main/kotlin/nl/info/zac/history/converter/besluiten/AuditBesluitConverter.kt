/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.besluiten

import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.shared.model.audit.AuditWijziging
import nl.info.zac.history.converter.addHistorieRegel
import nl.info.zac.history.model.HistoryLine

fun AuditWijziging<Besluit>.toHistoryLines(): List<HistoryLine> {
    val oud = this.oud
    val nieuw = this.nieuw

    if (oud == null || nieuw == null) {
        return listOf(HistoryLine("Besluit", oud?.identificatie, nieuw?.identificatie))
    }

    return mutableListOf<HistoryLine>().apply {
        addHistorieRegel("identificatie", oud.identificatie, nieuw.identificatie)
        addHistorieRegel("verzenddatum", oud.verzenddatum, nieuw.verzenddatum)
        addHistorieRegel("ingangsdatum", oud.ingangsdatum, nieuw.ingangsdatum)
        addHistorieRegel("vervaldatum", oud.vervaldatum, nieuw.vervaldatum)
        addHistorieRegel("toelichting", oud.toelichting, nieuw.toelichting)
    }
}
