/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter.documenten

import net.atos.client.zgw.drc.model.generated.Gebruiksrechten
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.app.audit.model.RESTHistorieRegel

object AuditGebruiksrechtenWijzigingConverter {
    fun convert(wijziging: AuditWijziging<Gebruiksrechten>): List<RESTHistorieRegel> =
        listOf(
            RESTHistorieRegel(
                "indicatieGebruiksrecht",
                toWaarde(wijziging.oud),
                toWaarde(wijziging.nieuw)
            )
        )

    private fun toWaarde(gebruiksrechten: Gebruiksrechten?): String? =
        gebruiksrechten?.omschrijvingVoorwaarden
}
