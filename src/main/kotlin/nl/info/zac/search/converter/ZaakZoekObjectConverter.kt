/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import jakarta.inject.Inject
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.zac.util.time.convertToDate
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.Rol
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isDeelzaak
import nl.info.client.zgw.zrc.util.isHeropend
import nl.info.client.zgw.zrc.util.isHoofdzaak
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.client.zgw.zrc.util.isVerlengd
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.getFullName
import nl.info.zac.search.model.ZaakIndicatie
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.search.model.zoekobject.toBetrokkeneIdentification
import nl.info.zac.search.model.zoekobject.toSolrFormatting
import java.util.UUID

class ZaakZoekObjectConverter @Inject constructor(
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,
    private val zgwApiService: ZgwApiService,
    private val identityService: IdentityService,
    private val flowableTaskService: FlowableTaskService
) : AbstractZoekObjectConverter<ZaakZoekObject>() {

    override fun convert(id: String): ZaakZoekObject =
        convert(id, zrcClientService::isZaakspecifiekGeautoriseerd)

    /**
     * Converts [id], looking up the zaakspecifiek geautoriseerd flag through [isZaakspecifiekGeautoriseerd]
     * instead of always calling [ZrcClientService.isZaakspecifiekGeautoriseerd] directly. Used by
     * [nl.info.zac.search.IndexingService] to memoize that lookup per zaak UUID across the taken of one zaak.
     */
    override fun convert(id: String, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): ZaakZoekObject {
        val zaak = zrcClientService.readZaak(UUID.fromString(id))
        return convert(zaak, isZaakspecifiekGeautoriseerd)
    }

    override fun supports(objectType: ZoekObjectType) = objectType == ZoekObjectType.ZAAK

    @Suppress("LongMethod")
    private fun convert(zaak: Zaak, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): ZaakZoekObject {
        val roles = zrcClientService.listRollen(zaak)
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val zaakZoekObject = ZaakZoekObject(
            id = zaak.uuid.toString(),
            type = ZoekObjectType.ZAAK.name,
            identificatie = zaak.identificatie,
            zaaktypeIdentificatie = zaaktype.identificatie,
            zaaktypeOmschrijving = zaaktype.omschrijving,
            zaaktypeUuid = zaaktype.url.extractUuid().toString()
        ).apply {
            this.isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd(zaak.uuid)
            omschrijving = zaak.omschrijving
            toelichting = zaak.toelichting
            registratiedatum = zaak.registratiedatum?.let(::convertToDate)
            startdatum = zaak.startdatum?.let(::convertToDate)
            einddatumGepland = zaak.einddatumGepland?.let(::convertToDate)
            einddatum = zaak.einddatum?.let(::convertToDate)
            uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening?.let(::convertToDate)
            publicatiedatum = zaak.publicatiedatum?.let(::convertToDate)
            // we use the name of this enum in the search index
            vertrouwelijkheidaanduiding = zaak.vertrouwelijkheidaanduiding.name
            isAfgehandeld = !zaak.isOpen()
            zgwApiService.findInitiatorRoleForZaak(zaak, roles)?.also(::setInitiator)
            // locatie is not yet supported
            locatie = null
            communicatiekanaal = zaak.communicatiekanaalNaam
            archiefActiedatum = zaak.archiefactiedatum?.let(::convertToDate)
            if (zaak.isVerlengd()) {
                setIndicatie(ZaakIndicatie.VERLENGD, true)
                duurVerlenging = zaak.verlenging.duur
                redenVerlenging = zaak.verlenging.reden
            }
            if (zaak.isOpgeschort()) {
                redenOpschorting = zaak.opschorting.reden
                setIndicatie(ZaakIndicatie.OPSCHORTING, true)
            }
            // we use the name of this enum in the search index (i.e., capitalized)
            zaak.archiefnominatie?.let { archiefNominatie = it.name }
            setIndicatie(ZaakIndicatie.DEELZAAK, zaak.isDeelzaak())
            setIndicatie(ZaakIndicatie.HOOFDZAAK, zaak.isHoofdzaak())
        }
        addBetrokkenen(roles, zaakZoekObject)
        findGroup(zaak, roles)?.let {
            zaakZoekObject.groepID = it.name
            zaakZoekObject.groepNaam = it.description
        }
        findBehandelaar(zaak, roles)?.let {
            zaakZoekObject.behandelaarNaam = it.getFullName()
            zaakZoekObject.behandelaarGebruikersnaam = it.id
            zaakZoekObject.isToegekend = true
        }
        zaak.status?.let {
            val status = zrcClientService.readStatus(it)
            zaakZoekObject.statusToelichting = status.statustoelichting
            zaakZoekObject.statusDatumGezet = convertToDate(status.datumStatusGezet)
            val statustype = ztcClientService.readStatustype(status.statustype)
            zaakZoekObject.statustypeOmschrijving = statustype.omschrijving
            zaakZoekObject.isStatusEindstatus = statustype.isEindstatus
            zaakZoekObject.setIndicatie(ZaakIndicatie.HEROPEND, statustype.isHeropend())
        }
        zaakZoekObject.aantalOpenstaandeTaken = flowableTaskService.countOpenTasksForZaak(zaak.uuid)
        zaak.resultaat?.let { zaakResultaat ->
            zrcClientService.readResultaat(zaakResultaat).let { resultaat ->
                ztcClientService.readResultaattype(resultaat.resultaattype).let { resultaattype ->
                    zaakZoekObject.resultaattypeOmschrijving = resultaattype.omschrijving
                    zaakZoekObject.resultaatToelichting = resultaat.toelichting
                }
            }
        }
        zaakZoekObject.bagObjectIDs = getBagObjectIDs(zaak)
        return zaakZoekObject
    }

    private fun addBetrokkenen(roles: List<Rol<*>>, zaakZoekObject: ZaakZoekObject) {
        for (role in roles) {
            // It is possible for a role in the ZGW zaakregister to not have an identification number.
            // This can happen when a rol for some reason no longer has an underlying 'identity' object (like a Natuurlijk Persoon etc.).
            // In this case, we treat the rol as an empty 'orphaned' role and ignore it here.
            role.toBetrokkeneIdentification()?.run {
                zaakZoekObject.addBetrokkene(
                    rol = role.omschrijving.orEmpty(),
                    identificatie = this.toSolrFormatting()
                )
            }
        }
    }

    private fun findBehandelaar(zaak: Zaak, roles: List<Rol<*>>): User? =
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak, roles)
            ?.betrokkeneIdentificatie
            ?.identificatie
            ?.let(identityService::readUser)

    private fun findGroup(zaak: Zaak, roles: List<Rol<*>>): Group? =
        zgwApiService.findGroepForZaak(zaak, roles)
            ?.betrokkeneIdentificatie
            ?.identificatie
            ?.let(identityService::readGroup)

    private fun getBagObjectIDs(zaak: Zaak): List<String> {
        val zaakobjectListParameters = ZaakobjectListParameters().apply { this.zaak = zaak.url }
        return zrcClientService.listZaakobjecten(zaakobjectListParameters)
            .results()
            .filter { it.isBagObject }
            .mapNotNull { it.waarde }
            .let { it.ifEmpty { emptyList() } }
    }
}
