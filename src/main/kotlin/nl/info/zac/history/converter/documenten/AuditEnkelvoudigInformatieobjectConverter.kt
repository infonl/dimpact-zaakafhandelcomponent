/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.history.converter.documenten

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.Ondertekening
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.history.converter.addHistorieRegel
import nl.info.zac.history.model.HistoryLine
import org.apache.commons.lang3.ObjectUtils
import java.net.URI
import java.time.LocalDate

class AuditEnkelvoudigInformatieobjectConverter @Inject constructor(
    private val ztcClientService: ZtcClientService
) {
    fun convert(wijziging: AuditWijziging<EnkelvoudigInformatieObject>): List<HistoryLine> {
        val oud = wijziging.oud
        val nieuw = wijziging.nieuw

        if (oud == null || nieuw == null) {
            return listOf(HistoryLine("informatieobject", toWaarde(oud), toWaarde(nieuw)))
        }

        return mutableListOf<HistoryLine>().apply {
            addHistorieRegel("titel", oud.titel, nieuw.titel)
            addHistorieRegel("identificatie", oud.identificatie, nieuw.identificatie)
            addHistorieRegel(
                "vertrouwelijkheidaanduiding",
                oud.vertrouwelijkheidaanduiding,
                nieuw.vertrouwelijkheidaanduiding
            )
            addHistorieRegel("bestandsnaam", oud.bestandsnaam, nieuw.bestandsnaam)
            addHistorieRegel("taal", oud.taal, nieuw.taal)
            addHistorieRegel("documentType", oud.informatieobjecttype, nieuw.informatieobjecttype)
            addHistorieRegel("auteur", oud.auteur, nieuw.auteur)
            addHistorieRegel("ontvangstdatum", oud.ontvangstdatum, nieuw.ontvangstdatum)
            addHistorieRegel(
                "registratiedatum",
                oud.beginRegistratie.toZonedDateTime(),
                nieuw.beginRegistratie.toZonedDateTime()
            )
            addHistorieRegel("locked", oud.locked, nieuw.locked)
            addHistorieRegel("versie", oud.versie.toString(), nieuw.versie.toString())
            addHistorieRegel("informatieobject.status", oud.status, nieuw.status)
            addHistorieRegel("bronorganisatie", oud.bronorganisatie, nieuw.bronorganisatie)
            addHistorieRegel("verzenddatum", oud.verzenddatum, nieuw.verzenddatum)
            addHistorieRegel("formaat", oud.formaat, nieuw.formaat)
            addHistorieRegel("ondertekening", toWaarde(oud.ondertekening), toWaarde(nieuw.ondertekening))
            addHistorieRegel("creatiedatum", oud.creatiedatum, nieuw.creatiedatum)
        }
    }

    private fun MutableList<HistoryLine>.addHistorieRegel(
        label: String,
        oud: URI,
        nieuw: URI
    ) {
        if (ObjectUtils.notEqual(oud, nieuw)) {
            this.add(HistoryLine(label, informatieobjecttypeToWaarde(oud), informatieobjecttypeToWaarde(nieuw)))
        }
    }

    private fun informatieobjecttypeToWaarde(informatieobjecttype: URI?): String? =
        if (informatieobjecttype != null) {
            ztcClientService.readInformatieobjecttype(informatieobjecttype).omschrijving
        } else {
            null
        }

    private fun toWaarde(enkelvoudigInformatieobject: EnkelvoudigInformatieObject?): String? =
        enkelvoudigInformatieobject?.identificatie

    private fun toWaarde(ondertekening: Ondertekening?): LocalDate? =
        ondertekening?.datum
}
