/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.search.converter

import jakarta.inject.Inject
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.search.model.DocumentIndicatie
import net.atos.zac.search.model.zoekobject.DocumentZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import net.atos.zac.util.time.DateTimeConverterUtil.convertToDate
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import java.util.UUID

class DocumentZoekObjectConverter @Inject constructor(
    private val identityService: IdentityService,
    private val brcClientService: BrcClientService,
    private val ztcClientService: ZtcClientService,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
    private val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService
) : AbstractZoekObjectConverter<DocumentZoekObject>() {

    override fun convert(id: String): DocumentZoekObject? {
        val document = drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(id))
        val zaakInformatieobject = zrcClientService.listZaakinformatieobjecten(document).firstOrNull() ?: return null
        return convert(document, zaakInformatieobject)
    }

    override fun supports(objectType: ZoekObjectType) = objectType == ZoekObjectType.DOCUMENT

    @Suppress("LongMethod")
    private fun convert(
        informatieobject: EnkelvoudigInformatieObject,
        gekoppeldeZaakInformatieobject: ZaakInformatieobject
    ): DocumentZoekObject {
        val zaak = zrcClientService.readZaak(gekoppeldeZaakInformatieobject.zaakUUID)
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
            gekoppeldeZaakInformatieobject.aardRelatieWeergave?.let { zaakRelatie = it.toValue() }
            isZaakAfgehandeld = zaak.isOpen
            creatiedatum = convertToDate(informatieobject.creatiedatum)
            registratiedatum = convertToDate(informatieobject.beginRegistratie.toZonedDateTime())
            ontvangstdatum = convertToDate(informatieobject.ontvangstdatum)
            verzenddatum = convertToDate(informatieobject.verzenddatum)
            ondertekeningDatum = convertToDate(informatieobject.ontvangstdatum)
            // we use the name of this enum in the search index
            vertrouwelijkheidaanduiding = informatieobject.vertrouwelijkheidaanduiding.name
            auteur = informatieobject.auteur
            informatieobject.status?.let(::setStatus)
            formaat = informatieobject.formaat
            versie = informatieobject.versie.toLong()
            bestandsnaam = informatieobject.bestandsnaam
            bestandsomvang = informatieobject.bestandsomvang.toLong()
            documentType = informatieobjecttype.omschrijving
            informatieobject.ondertekening?.let { ondertekening ->
                ondertekening.soort?.let {
                    ondertekeningSoort = it.toString()
                }
                ondertekeningDatum = convertToDate(ondertekening.datum)
                setIndicatie(DocumentIndicatie.ONDERTEKEND, true)
            }
            setIndicatie(DocumentIndicatie.VERGRENDELD, informatieobject.locked)
            setIndicatie(DocumentIndicatie.GEBRUIKSRECHT, informatieobject.indicatieGebruiksrecht)
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
