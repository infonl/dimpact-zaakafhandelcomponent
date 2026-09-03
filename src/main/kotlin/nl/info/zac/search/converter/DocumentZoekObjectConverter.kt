/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.converter

import jakarta.inject.Inject
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakInformatieObject
import nl.info.client.zgw.zrc.model.zaakUUID
import nl.info.zac.util.time.convertToDate
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import java.util.UUID

class DocumentZoekObjectConverter @Inject constructor(
    private val identityService: IdentityService,
    private val brcClientService: BrcClientService,
    private val ztcClientService: ZtcClientService,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService
) : AbstractZoekObjectConverter<DocumentZoekObject>() {

    override fun convert(id: String): DocumentZoekObject? =
        convert(id, zrcClientService::isZaakspecifiekGeautoriseerd)

    /**
     * Converts [id], looking up the zaakspecifiek geautoriseerd flag through [isZaakspecifiekGeautoriseerd]
     * for whichever zaak the document actually resolves against, rather than always calling
     * [ZrcClientService.isZaakspecifiekGeautoriseerd] directly. Used by
     * [nl.info.zac.search.IndexingService.addOrUpdateInformatieobjectenForZaak] to memoize that lookup
     * per zaak UUID across the documents of one zaak.
     */
    override fun convert(id: String, isZaakspecifiekGeautoriseerd: (UUID) -> Boolean): DocumentZoekObject? {
        val document = drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(id))
        val zaakInformatieobject = zrcClientService.listZaakinformatieobjecten(document).firstOrNull() ?: return null
        val zaak = zrcClientService.readZaak(zaakInformatieobject.zaakUUID)
        return convert(document, zaak, zaakInformatieobject, isZaakspecifiekGeautoriseerd)
    }

    /**
     * Converts the document identified by [zaakInformatieobject], using the already-retrieved [zaak] it
     * links to, instead of independently resolving both a zaak (via [ZrcClientService.readZaak]) and a
     * `ZaakInformatieObject` (via a reverse `listZaakinformatieobjecten` lookup on the document, which
     * could resolve to a different zaak than [zaak] for a document linked to more than one zaak). Used by
     * [nl.info.zac.search.IndexingService]'s zaak-driven combined reindex, which already retrieved both
     * while listing [zaak]'s own linked documenten - so, unlike [convert]'s other overloads, there is no
     * "document has no linked zaak" case to signal here.
     */
    fun convert(
        zaakInformatieobject: ZaakInformatieObject,
        zaak: Zaak,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): DocumentZoekObject {
        val document = drcClientService.readEnkelvoudigInformatieobject(zaakInformatieobject.informatieobject.extractUuid())
        return convert(document, zaak, zaakInformatieobject, isZaakspecifiekGeautoriseerd)
    }

    override fun supports(objectType: ZoekObjectType) = objectType == ZoekObjectType.DOCUMENT

    @Suppress("LongMethod")
    private fun convert(
        informatieobject: EnkelvoudigInformatieObject,
        zaak: Zaak,
        gekoppeldeZaakInformatieobject: ZaakInformatieObject,
        isZaakspecifiekGeautoriseerd: (UUID) -> Boolean
    ): DocumentZoekObject {
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val informatieobjecttype = ztcClientService.readInformatieobjecttype(informatieobject.informatieobjecttype)
        val informatieobjectUUID = informatieobject.url.extractUuid()
        return DocumentZoekObject(
            id = informatieobjectUUID.toString(),
            type = ZoekObjectType.DOCUMENT.name
        ).apply {
            identificatie = informatieobject.identificatie
            titel = informatieobject.titel
            beschrijving = informatieobject.beschrijving
            zaaktypeOmschrijving = zaaktype.omschrijving
            zaaktypeUuid = zaaktype.url.extractUuid().toString()
            zaaktypeIdentificatie = zaaktype.identificatie
            zaakIdentificatie = zaak.identificatie
            zaakUuid = zaak.uuid.toString()
            this.isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd(zaak.uuid)
            gekoppeldeZaakInformatieobject.aardRelatieWeergave?.let { zaakRelatie = it.toString() }
            isZaakAfgehandeld = !zaak.isOpen()
            creatiedatum = convertToDate(informatieobject.creatiedatum)
            registratiedatum = convertToDate(informatieobject.beginRegistratie.toZonedDateTime())
            ontvangstdatum = informatieobject.ontvangstdatum?.let(::convertToDate)
            verzenddatum = informatieobject.verzenddatum?.let(::convertToDate)
            ondertekeningDatum = informatieobject.ontvangstdatum?.let(::convertToDate)
            // we use the name of this enum in the search index
            vertrouwelijkheidaanduiding = informatieobject.vertrouwelijkheidaanduiding.name
            auteur = informatieobject.auteur
            informatieobject.status?.let(::setStatus)
            formaat = informatieobject.formaat
            versie = informatieobject.versie.toLong()
            bestandsnaam = informatieobject.bestandsnaam
            bestandsomvang = informatieobject.bestandsomvang?.toLong() ?: 0
            documentType = informatieobjecttype.omschrijving
            informatieobject.ondertekening?.let { ondertekening ->
                ondertekening.soort?.let {
                    ondertekeningSoort = it.toString()
                }
                ondertekeningDatum = convertToDate(ondertekening.datum)
                setIndicatie(DocumentIndicatie.ONDERTEKEND, true)
            }
            setIndicatie(DocumentIndicatie.VERGRENDELD, informatieobject.locked)
            // indicatieGebruiksRecht may be `null` according to the ZGW API specification,
            // where a null value indicates that it is not known yet
            informatieobject.indicatieGebruiksrecht?.let {
                setIndicatie(DocumentIndicatie.GEBRUIKSRECHT, it)
            }
            setIndicatie(
                DocumentIndicatie.BESLUIT,
                brcClientService.isInformatieObjectGekoppeldAanBesluit(informatieobject.url)
            )
            setIndicatie(DocumentIndicatie.VERZONDEN, informatieobject.verzenddatum != null)
            if (informatieobject.locked) {
                enkelvoudigInformatieObjectLockService.readLock(informatieobjectUUID).userId?.let {
                    vergrendeldDoorGebruikersnaam = it
                    vergrendeldDoorNaam = identityService.readUser(it).getFullName()
                }
            }
        }
    }
}
