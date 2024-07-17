/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.audit.converter.documenten

import net.atos.client.zgw.drc.model.generated.Gebruiksrechten
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.documenten.GebuiksrechtenWijziging
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel

class AuditGebruiksrechtenWijzigingConverter : AbstractAuditWijzigingConverter<GebuiksrechtenWijziging>() {
    override fun supports(objectType: ObjectType): Boolean = ObjectType.GEBRUIKSRECHTEN == objectType

    override fun doConvert(wijziging: GebuiksrechtenWijziging): List<RESTHistorieRegel> =
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
