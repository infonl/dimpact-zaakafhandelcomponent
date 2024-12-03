/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.getFullName
import net.atos.zac.util.time.DateTimeConverterUtil.convertToDate
import net.atos.zac.util.uuidFromURI
import net.atos.zac.zoeken.model.ZaakIndicatie
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
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
    override fun supports(objectType: ZoekObjectType) = objectType == ZoekObjectType.ZAAK

    @Suppress("LongMethod")
    private fun convert(zaak: Zaak): ZaakZoekObject {
        val zaakZoekObject = ZaakZoekObject(
            id = zaak.uuid.toString(),
            type = ZoekObjectType.ZAAK.name
        ).apply {
            identificatie = zaak.identificatie
            omschrijving = zaak.omschrijving
            toelichting = zaak.toelichting
            registratiedatum = convertToDate(zaak.registratiedatum)
            startdatum = convertToDate(zaak.startdatum)
            einddatumGepland = convertToDate(zaak.einddatumGepland)
            einddatum = convertToDate(zaak.einddatum)
            uiterlijkeEinddatumAfdoening = convertToDate(zaak.uiterlijkeEinddatumAfdoening)
            publicatiedatum = convertToDate(zaak.publicatiedatum)
            // we use the name of this enum in the search index
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name
            isAfgehandeld = !zaak.isOpen
            zgwApiService.findInitiatorRoleForZaak(zaak).ifPresent { setInitiator(it) }
            // locatie is not yet supported
            locatie = null
            communicatiekanaal = zaak.communicatiekanaalNaam
            archiefActiedatum = convertToDate(zaak.archiefactiedatum)
            if (zaak.isVerlengd) {
                setIndicatie(ZaakIndicatie.VERLENGD, true)
                duurVerlenging = zaak.verlenging.duur.toString()
                redenVerlenging = zaak.verlenging.reden
            }
            if (zaak.isOpgeschort) {
                redenOpschorting = zaak.opschorting.reden
                setIndicatie(ZaakIndicatie.OPSCHORTING, true)
            }
            zaak.archiefnominatie?.let { archiefNominatie = it.toString() }
            setIndicatie(ZaakIndicatie.DEELZAAK, zaak.isDeelzaak)
            setIndicatie(ZaakIndicatie.HOOFDZAAK, zaak.is_Hoofdzaak)
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
        ztcClientService.readZaaktype(zaak.zaaktype).let {
            zaakZoekObject.zaaktypeIdentificatie = it.identificatie
            zaakZoekObject.zaaktypeOmschrijving = it.omschrijving
            zaakZoekObject.zaaktypeUuid = uuidFromURI(it.url).toString()
        }
        zaak.status?.let {
            val status = zrcClientService.readStatus(it)
            zaakZoekObject.statusToelichting = status.statustoelichting
            zaakZoekObject.statusDatumGezet = convertToDate(status.datumStatusGezet)
            val statustype = ztcClientService.readStatustype(status.statustype)
            zaakZoekObject.statustypeOmschrijving = statustype.omschrijving
            zaakZoekObject.isStatusEindstatus = statustype.isEindstatus
            zaakZoekObject.setIndicatie(ZaakIndicatie.HEROPEND, StatusTypeUtil.isHeropend(statustype))
        }
        zaakZoekObject.aantalOpenstaandeTaken = flowableTaskService.countOpenTasksForZaak(zaak.uuid)
        zaak.resultaat?.let { zaakResultaat ->
            zrcClientService.readResultaat(zaakResultaat)?.let {
                val resultaattype = ztcClientService.readResultaattype(it.resultaattype)
                zaakZoekObject.resultaattypeOmschrijving = resultaattype.omschrijving
                zaakZoekObject.resultaatToelichting = it.toelichting
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
}
