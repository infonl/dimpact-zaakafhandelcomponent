package net.atos.zac.zoeken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.getFullName
import net.atos.zac.util.UriUtil
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import java.util.UUID

class ZaakZoekObjectConverter @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZGWApiService,
    private val identityService: IdentityService,
    private val flowableTaskService: FlowableTaskService
) : AbstractZoekObjectConverter<ZaakZoekObject>() {

    override fun convert(id: String): ZaakZoekObject {
        val zaak = zrcClientService.readZaak(UUID.fromString(id))
        return convert(zaak)
    }

    @Suppress("LongMethod")
    private fun convert(zaak: Zaak): ZaakZoekObject {
        val zaakZoekObject = ZaakZoekObject().apply {
            uuid = zaak.uuid.toString()
            type = ZoekObjectType.ZAAK
            identificatie = zaak.identificatie
            omschrijving = zaak.omschrijving
            toelichting = zaak.toelichting
            registratiedatum = DateTimeConverterUtil.convertToDate(zaak.registratiedatum)
            startdatum = DateTimeConverterUtil.convertToDate(zaak.startdatum)
            einddatumGepland = DateTimeConverterUtil.convertToDate(zaak.einddatumGepland)
            einddatum = DateTimeConverterUtil.convertToDate(zaak.einddatum)
            uiterlijkeEinddatumAfdoening = DateTimeConverterUtil.convertToDate(zaak.uiterlijkeEinddatumAfdoening)
            publicatiedatum = DateTimeConverterUtil.convertToDate(zaak.publicatiedatum)
            // we use the uppercase version of this enum in the ZAC backend API
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name
            isAfgehandeld = !zaak.isOpen
            zgwApiService.findInitiatorRoleForZaak(zaak).ifPresent { setInitiator(it) }
            // locatie is not yet supported
            locatie = null
            communicatiekanaal = zaak.communicatiekanaalNaam
            archiefActiedatum = DateTimeConverterUtil.convertToDate(zaak.archiefactiedatum)
        }
        addBetrokkenen(zaak, zaakZoekObject)

        findGroup(zaak)?.let {
            zaakZoekObject.groepID = it.id
            zaakZoekObject.groepNaam = it.name
        }
        findBehandelaar(zaak)?.let {
            zaakZoekObject.behandelaarNaam = it.getFullName()
            zaakZoekObject.behandelaarGebruikersnaam = it.id
            zaakZoekObject.isToegekend = true
        }

        if (zaak.isVerlengd) {
            zaakZoekObject.setIndicatie(ZaakIndicatie.VERLENGD, true)
            zaakZoekObject.duurVerlenging = zaak.verlenging.duur.toString()
            zaakZoekObject.redenVerlenging = zaak.verlenging.reden
        }

        if (zaak.isOpgeschort) {
            zaakZoekObject.redenOpschorting = zaak.opschorting.reden
            zaakZoekObject.setIndicatie(ZaakIndicatie.OPSCHORTING, true)
        }

        if (zaak.archiefnominatie != null) {
            zaakZoekObject.archiefNominatie = zaak.archiefnominatie.toString()
        }

        zaakZoekObject.setIndicatie(ZaakIndicatie.DEELZAAK, zaak.isDeelzaak)
        zaakZoekObject.setIndicatie(ZaakIndicatie.HOOFDZAAK, zaak.is_Hoofdzaak)

        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        zaakZoekObject.zaaktypeIdentificatie = zaaktype.identificatie
        zaakZoekObject.zaaktypeOmschrijving = zaaktype.omschrijving
        zaakZoekObject.zaaktypeUuid = UriUtil.uuidFromURI(zaaktype.url).toString()

        if (zaak.status != null) {
            val status = zrcClientService.readStatus(zaak.status)
            zaakZoekObject.statusToelichting = status.statustoelichting
            zaakZoekObject.statusDatumGezet = DateTimeConverterUtil.convertToDate(status.datumStatusGezet)
            val statustype = ztcClientService.readStatustype(status.statustype)
            zaakZoekObject.statustypeOmschrijving = statustype.omschrijving
            zaakZoekObject.isStatusEindstatus = statustype.isEindstatus
            zaakZoekObject.setIndicatie(ZaakIndicatie.HEROPEND, StatusTypeUtil.isHeropend(statustype))
        }

        zaakZoekObject.aantalOpenstaandeTaken = flowableTaskService.countOpenTasksForZaak(zaak.uuid)

        if (zaak.resultaat != null) {
            val resultaat = zrcClientService.readResultaat(zaak.resultaat)
            if (resultaat != null) {
                val resultaattype = ztcClientService.readResultaattype(resultaat.resultaattype)
                zaakZoekObject.resultaattypeOmschrijving = resultaattype.omschrijving
                zaakZoekObject.resultaatToelichting = resultaat.toelichting
            }
        }
        zaakZoekObject.bagObjectIDs = getBagObjectIDs(zaak)

        return zaakZoekObject
    }

    private fun addBetrokkenen(zaak: Zaak, zaakZoekObject: ZaakZoekObject) {
        for (rol in zrcClientService.listRollen(zaak)) {
            zaakZoekObject.addBetrokkene(rol.omschrijving, rol.identificatienummer)
        }
    }

    private fun findBehandelaar(zaak: Zaak): User? =
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
            .map { identityService.readUser(it.betrokkeneIdentificatie.identificatie) }
            .orElse(null)

    private fun findGroup(zaak: Zaak): Group? =
        zgwApiService.findGroepForZaak(zaak)
            .map { identityService.readGroup(it.betrokkeneIdentificatie.identificatie) }
            .orElse(null)

    private fun getBagObjectIDs(zaak: Zaak): List<String> {
        val zaakobjectListParameters = ZaakobjectListParameters().apply { this.zaak = zaak.url }
        val zaakobjecten = zrcClientService.listZaakobjecten(zaakobjectListParameters)
        return zaakobjecten.results
            .filter { it.isBagObject }
            .map { it.waarde }
            .takeIf { it.isNotEmpty() } ?: emptyList()
    }

    override fun supports(objectType: ZoekObjectType): Boolean = objectType == ZoekObjectType.ZAAK
}
