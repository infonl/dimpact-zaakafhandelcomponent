/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.audit.converter.besluiten

import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.besluiten.BesluitWijziging
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.util.LinkedList

class AuditBesluitConverter : AbstractAuditWijzigingConverter<BesluitWijziging>() {
    override fun supports(objectType: ObjectType): Boolean =
        ObjectType.BESLUIT == objectType

    override fun doConvert(wijziging: BesluitWijziging): List<RESTHistorieRegel> {
        val oud = wijziging.oud
        val nieuw = wijziging.nieuw

        if (oud == null || nieuw == null) {
            return listOf(RESTHistorieRegel("Besluit", toWaarde(oud), toWaarde(nieuw)))
        }

        val historieRegels = LinkedList<RESTHistorieRegel>()
        checkAttribuut("identificatie", oud.identificatie, nieuw.identificatie, historieRegels)
        checkAttribuut("verzenddatum", oud.verzenddatum, nieuw.verzenddatum, historieRegels)
        checkAttribuut("ingangsdatum", oud.ingangsdatum, nieuw.ingangsdatum, historieRegels)
        checkAttribuut("vervaldatum", oud.vervaldatum, nieuw.vervaldatum, historieRegels)
        checkAttribuut("toelichting", oud.toelichting, nieuw.toelichting, historieRegels)
        return historieRegels
    }

    private fun toWaarde(besluit: Besluit?): String? =
        besluit?.identificatie
}
