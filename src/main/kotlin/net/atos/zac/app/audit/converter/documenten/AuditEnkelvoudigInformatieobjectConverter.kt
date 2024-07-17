package net.atos.zac.app.audit.converter.documenten

import jakarta.inject.Inject
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.Ondertekening
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.documenten.EnkelvoudigInformatieobjectWijziging
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel
import org.apache.commons.lang3.ObjectUtils
import java.net.URI
import java.time.LocalDate
import java.util.LinkedList
import java.util.stream.Stream

class AuditEnkelvoudigInformatieobjectConverter :
    AbstractAuditWijzigingConverter<EnkelvoudigInformatieobjectWijziging>() {
    @Inject
    lateinit var ztcClientService: ZtcClientService

    override fun supports(objectType: ObjectType): Boolean =
        ObjectType.ENKELVOUDIG_INFORMATIEOBJECT == objectType

    override fun doConvert(wijziging: EnkelvoudigInformatieobjectWijziging): Stream<RESTHistorieRegel> {
        val oud = wijziging.oud
        val nieuw = wijziging.nieuw

        if (oud == null || nieuw == null) {
            return Stream.of(RESTHistorieRegel("informatieobject", toWaarde(oud), toWaarde(nieuw)))
        }

        val historieRegels: MutableList<RESTHistorieRegel> = LinkedList()
        checkAttribuut("titel", oud.titel, nieuw.titel, historieRegels)
        checkAttribuut("identificatie", oud.identificatie, nieuw.identificatie, historieRegels)
        checkAttribuut(
            "vertrouwelijkheidaanduiding",
            oud.vertrouwelijkheidaanduiding,
            nieuw.vertrouwelijkheidaanduiding,
            historieRegels
        )
        checkAttribuut("bestandsnaam", oud.bestandsnaam, nieuw.bestandsnaam, historieRegels)
        checkAttribuut("taal", oud.taal, nieuw.taal, historieRegels)
        checkInformatieobjecttype(oud.informatieobjecttype, nieuw.informatieobjecttype, historieRegels)
        checkAttribuut("auteur", oud.auteur, nieuw.auteur, historieRegels)
        checkAttribuut("ontvangstdatum", oud.ontvangstdatum, nieuw.ontvangstdatum, historieRegels)
        checkAttribuut(
            "registratiedatum",
            oud.beginRegistratie.toZonedDateTime(),
            nieuw.beginRegistratie.toZonedDateTime(),
            historieRegels
        )
        checkAttribuut("locked", oud.locked, nieuw.locked, historieRegels)
        checkAttribuut("versie", oud.versie.toString(), nieuw.versie.toString(), historieRegels)
        checkAttribuut("informatieobject.status", oud.status, nieuw.status, historieRegels)
        checkAttribuut("bronorganisatie", oud.bronorganisatie, nieuw.bronorganisatie, historieRegels)
        checkAttribuut("verzenddatum", oud.verzenddatum, nieuw.verzenddatum, historieRegels)
        checkAttribuut("formaat", oud.formaat, nieuw.formaat, historieRegels)
        checkAttribuut("ondertekening", toWaarde(oud.ondertekening), toWaarde(nieuw.ondertekening), historieRegels)
        checkAttribuut("creatiedatum", oud.creatiedatum, nieuw.creatiedatum, historieRegels)
        return historieRegels.stream()
    }

    private fun checkInformatieobjecttype(oud: URI, nieuw: URI, historieRegels: MutableList<RESTHistorieRegel>) {
        if (ObjectUtils.notEqual(oud, nieuw)) {
            historieRegels.add(
                RESTHistorieRegel(
                    "documentType",
                    informatieobjecttypeToWaarde(oud),
                    informatieobjecttypeToWaarde(nieuw)
                )
            )
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
