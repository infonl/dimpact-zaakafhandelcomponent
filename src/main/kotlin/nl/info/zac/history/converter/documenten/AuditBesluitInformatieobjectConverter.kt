/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.history.converter.documenten

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.besluiten.BesluitInformatieobjectWijziging
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.history.model.HistoryLine

class AuditBesluitInformatieobjectConverter @Inject constructor(
    private val brcClientService: BrcClientService
) {

    fun convert(wijziging: BesluitInformatieobjectWijziging): List<HistoryLine> =
        listOf(HistoryLine("informatieobject", toWaarde(wijziging.oud), toWaarde(wijziging.nieuw)))

    private fun toWaarde(besluitInformatieObject: BesluitInformatieObject?): String? =
        besluitInformatieObject?.let {
            brcClientService.readBesluit(it.besluit.extractUuid()).identificatie
        }
}
