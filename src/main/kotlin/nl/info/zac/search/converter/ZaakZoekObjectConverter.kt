/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.util.time.DateTimeConverterUtil.convertToDate
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.getFullName
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
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
            zgwApiService.findInitiatorRoleForZaak(zaak)?.also(::setInitiator)
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
            zaakZoekObject.zaaktypeUuid = it.url.extractUuid().toString()
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
            zrcClientService.readResultaat(zaakResultaat)?.let { resultaat ->
                ztcClientService.readResultaattype(resultaat.resultaattype).let { resultaattype ->
                    zaakZoekObject.resultaattypeOmschrijving = resultaattype.omschrijving
                    zaakZoekObject.resultaatToelichting = resultaat.toelichting
                }
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
            ?.betrokkeneIdentificatie
            ?.identificatie
            ?.let(identityService::readUser)

    private fun findGroup(zaak: Zaak): Group? =
        zgwApiService.findGroepForZaak(zaak)
            ?.betrokkeneIdentificatie
            ?.identificatie
            ?.let(identityService::readGroup)

    private fun getBagObjectIDs(zaak: Zaak): List<String> {
        val zaakobjectListParameters = ZaakobjectListParameters().apply { this.zaak = zaak.url }
        return zrcClientService.listZaakobjecten(zaakobjectListParameters)
            .results
            .filter { it.isBagObject }
            .map { it.waarde }
            .let { if (it.isNotEmpty()) it else emptyList() }
    }
}
