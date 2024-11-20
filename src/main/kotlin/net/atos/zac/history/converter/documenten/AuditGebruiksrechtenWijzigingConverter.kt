/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.history.converter.documenten

import net.atos.client.zgw.drc.model.generated.Gebruiksrechten
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.history.model.HistoryLine

object AuditGebruiksrechtenWijzigingConverter {
    fun convert(wijziging: AuditWijziging<Gebruiksrechten>): List<HistoryLine> =
        listOf(
            HistoryLine(
                "indicatieGebruiksrecht",
                toWaarde(wijziging.oud),
                toWaarde(wijziging.nieuw)
            )
        )

    private fun toWaarde(gebruiksrechten: Gebruiksrechten?): String? =
        gebruiksrechten?.omschrijvingVoorwaarden
}
