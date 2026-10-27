/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter.documenten

import nl.info.client.zgw.drc.model.generated.Gebruiksrechten
import nl.info.client.zgw.shared.model.audit.AuditWijziging
import nl.info.zac.history.model.HistoryLine

fun AuditWijziging<Gebruiksrechten>.toHistoryLines(): List<HistoryLine> =
    listOf(
        HistoryLine(
            "indicatieGebruiksrecht",
            oud?.omschrijvingVoorwaarden,
            nieuw?.omschrijvingVoorwaarden
        )
    )
