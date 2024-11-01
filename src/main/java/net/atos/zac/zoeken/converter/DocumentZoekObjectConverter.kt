package net.atos.zac.zoeken.converter

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.zoeken.model.DocumentIndicatie
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject
import java.util.UUID

class DocumentZoekObjectConverter @Inject constructor(
    val identityService: IdentityService,
    val brcClientService: BrcClientService,
    val ztcClientService: ZtcClientService,
    val drcClientService: DrcClientService,
    val zrcClientService: ZrcClientService,
    val enkelvoudigInformatieObjectLockService: EnkelvoudigInformatieObjectLockService
) : AbstractZoekObjectConverter<DocumentZoekObject>() {

    override fun convert(id: String): DocumentZoekObject? {
        val document = drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(id))
        val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(document)
        if (zaakInformatieobjecten.isEmpty()) {
            return null
        }
        return convert(document, zaakInformatieobjecten.first())
    }

    @Suppress("LongMethod")
    private fun convert(
        informatieobject: EnkelvoudigInformatieObject,
        gekoppeldeZaakInformatieobject: ZaakInformatieobject
    ): DocumentZoekObject {
        val zaak = zrcClientService.readZaak(gekoppeldeZaakInformatieobject.zaakUUID)
        val zaaktype = ztcClientService.readZaaktype(zaak.zaaktype)
        val informatieobjecttype = ztcClientService.readInformatieobjecttype(
            informatieobject.informatieobjecttype
        )
        val documentZoekObject = DocumentZoekObject()
        val informatieobjectUUID = URIUtil.parseUUIDFromResourceURI(informatieobject.url)
        documentZoekObject.type = ZoekObjectType.DOCUMENT
        documentZoekObject.uuid = informatieobjectUUID.toString()
        documentZoekObject.identificatie = informatieobject.identificatie
        documentZoekObject.titel = informatieobject.titel
        documentZoekObject.beschrijving = informatieobject.beschrijving
        documentZoekObject.zaaktypeOmschrijving = zaaktype.omschrijving
        documentZoekObject.zaaktypeUuid = URIUtil.parseUUIDFromResourceURI(zaaktype.url).toString()
        documentZoekObject.zaaktypeIdentificatie = zaaktype.identificatie
        documentZoekObject.zaakIdentificatie = zaak.identificatie
        documentZoekObject.zaakUuid = zaak.uuid.toString()
        if (gekoppeldeZaakInformatieobject.aardRelatieWeergave != null) {
            documentZoekObject.zaakRelatie = gekoppeldeZaakInformatieobject.aardRelatieWeergave.toValue()
        }
        documentZoekObject.isZaakAfgehandeld = zaak.isOpen
        documentZoekObject.creatiedatum = DateTimeConverterUtil.convertToDate(informatieobject.creatiedatum)
        documentZoekObject.registratiedatum = DateTimeConverterUtil.convertToDate(
            informatieobject.beginRegistratie.toZonedDateTime()
        )
        documentZoekObject.ontvangstdatum = DateTimeConverterUtil.convertToDate(informatieobject.ontvangstdatum)
        documentZoekObject.verzenddatum = DateTimeConverterUtil.convertToDate(informatieobject.verzenddatum)
        documentZoekObject.ondertekeningDatum = DateTimeConverterUtil.convertToDate(informatieobject.ontvangstdatum)
        // we use the name of this enum in the search index
        documentZoekObject.vertrouwelijkheidaanduiding = informatieobject.vertrouwelijkheidaanduiding.name
        documentZoekObject.auteur = informatieobject.auteur
        if (informatieobject.status != null) {
            documentZoekObject.status = informatieobject.status
        }
        documentZoekObject.formaat = informatieobject.formaat
        documentZoekObject.versie = informatieobject.versie.toLong()
        documentZoekObject.bestandsnaam = informatieobject.bestandsnaam
        documentZoekObject.bestandsomvang = documentZoekObject.bestandsomvang
        documentZoekObject.inhoudUrl = documentZoekObject.inhoudUrl
        documentZoekObject.documentType = informatieobjecttype.omschrijving
        if (informatieobject.ondertekening != null) {
            if (informatieobject.ondertekening.soort != null) {
                documentZoekObject.ondertekeningSoort = informatieobject.ondertekening.soort.toString()
            }
            documentZoekObject.ondertekeningDatum =
                DateTimeConverterUtil.convertToDate(informatieobject.ondertekening.datum)
            documentZoekObject.setIndicatie(DocumentIndicatie.ONDERTEKEND, true)
        }
        documentZoekObject.setIndicatie(DocumentIndicatie.VERGRENDELD, informatieobject.locked)
        documentZoekObject.setIndicatie(DocumentIndicatie.GEBRUIKSRECHT, informatieobject.indicatieGebruiksrecht)
        documentZoekObject.setIndicatie(
            DocumentIndicatie.BESLUIT,
            brcClientService.isInformatieObjectGekoppeldAanBesluit(informatieobject.url)
        )
        documentZoekObject.setIndicatie(DocumentIndicatie.VERZONDEN, informatieobject.verzenddatum != null)
        if (informatieobject.locked) {
            enkelvoudigInformatieObjectLockService.readLock(informatieobjectUUID).userId?.let {
                documentZoekObject.vergrendeldDoorGebruikersnaam = it
                documentZoekObject.vergrendeldDoorNaam = identityService.readUser(it).getFullName()
            }
        }
        return documentZoekObject
    }

    override fun supports(objectType: ZoekObjectType): Boolean = objectType == ZoekObjectType.DOCUMENT
}
