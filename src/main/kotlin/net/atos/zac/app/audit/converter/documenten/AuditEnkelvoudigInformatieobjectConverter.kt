/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.audit.converter.documenten

import jakarta.inject.Inject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.documenten.EnkelvoudigInformatieobjectWijziging
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter
import net.atos.zac.app.audit.converter.addHistorieRegel
import net.atos.zac.app.audit.model.RESTHistorieRegel
import org.apache.commons.lang3.ObjectUtils
import java.net.URI
import java.time.LocalDate

class AuditEnkelvoudigInformatieobjectConverter :
    AbstractAuditWijzigingConverter<EnkelvoudigInformatieobjectWijziging>() {
    @Inject
    lateinit var ztcClientService: ZtcClientService

    override fun supports(objectType: ObjectType): Boolean =
        ObjectType.ENKELVOUDIG_INFORMATIEOBJECT == objectType

    override fun doConvert(wijziging: EnkelvoudigInformatieobjectWijziging): List<RESTHistorieRegel> {
        val oud = wijziging.oud
        val nieuw = wijziging.nieuw

        if (oud == null || nieuw == null) {
            return listOf(RESTHistorieRegel("informatieobject", toWaarde(oud), toWaarde(nieuw)))
        }

        return mutableListOf<RESTHistorieRegel>().apply {
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

    private fun MutableList<RESTHistorieRegel>.addHistorieRegel(
        label: String,
        oud: URI,
        nieuw: URI
    ) {
        if (ObjectUtils.notEqual(oud, nieuw)) {
            this.add(RESTHistorieRegel(label, informatieobjecttypeToWaarde(oud), informatieobjecttypeToWaarde(nieuw)))
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
