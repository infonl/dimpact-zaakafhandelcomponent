package net.atos.zac.zoeken.converter;

import static net.atos.client.zgw.shared.util.URIUtil.parseUUIDFromResourceURI;
import static net.atos.zac.identity.model.UserKt.getFullName;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import net.atos.client.zgw.brc.BrcClientService;
import net.atos.client.zgw.drc.DrcClientService;
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.client.zgw.shared.util.URIUtil;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.ZaakInformatieobject;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectLockService;
import net.atos.zac.enkelvoudiginformatieobject.model.EnkelvoudigInformatieObjectLock;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.util.time.DateTimeConverterUtil;
import net.atos.zac.zoeken.model.DocumentIndicatie;
import net.atos.zac.zoeken.model.index.ZoekObjectType;
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject;

public class DocumentZoekObjectConverter extends AbstractZoekObjectConverter<DocumentZoekObject> {

    @Inject
    private IdentityService identityService;

    @Inject
    private BrcClientService brcClientService;

    @Inject
    private ZtcClientService ztcClientService;

    @Inject
    private DrcClientService drcClientService;

    @Inject
    private ZrcClientService zrcClientService;

    @Inject
    private EnkelvoudigInformatieObjectLockService enkelvoudigInformatieObjectLockService;


    @Override
    public DocumentZoekObject convert(final String documentUUID) {
        final EnkelvoudigInformatieObject document = drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(documentUUID));
        final List<ZaakInformatieobject> zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(document);
        if (zaakInformatieobjecten.isEmpty()) {
            return null;
        }
        return convert(document, zaakInformatieobjecten.getFirst());
    }

    private DocumentZoekObject convert(
            final EnkelvoudigInformatieObject informatieobject,
            final ZaakInformatieobject gekoppeldeZaakInformatieobject
    ) {
        final Zaak zaak = zrcClientService.readZaak(gekoppeldeZaakInformatieobject.getZaakUUID());
        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());
        final InformatieObjectType informatieobjecttype = ztcClientService.readInformatieobjecttype(informatieobject
                .getInformatieobjecttype());
        final DocumentZoekObject documentZoekObject = new DocumentZoekObject();
        final UUID informatieobjectUUID = parseUUIDFromResourceURI(informatieobject.getUrl());
        documentZoekObject.setType(ZoekObjectType.DOCUMENT);
        documentZoekObject.setUuid(informatieobjectUUID.toString());
        documentZoekObject.setIdentificatie(informatieobject.getIdentificatie());
        documentZoekObject.setTitel(informatieobject.getTitel());
        documentZoekObject.setBeschrijving(informatieobject.getBeschrijving());
        documentZoekObject.setZaaktypeOmschrijving(zaaktype.getOmschrijving());
        documentZoekObject.setZaaktypeUuid(URIUtil.parseUUIDFromResourceURI(zaaktype.getUrl()).toString());
        documentZoekObject.setZaaktypeIdentificatie(zaaktype.getIdentificatie());
        documentZoekObject.setZaakIdentificatie(zaak.getIdentificatie());
        documentZoekObject.setZaakUuid(zaak.getUuid().toString());
        if (gekoppeldeZaakInformatieobject.getAardRelatieWeergave() != null) {
            documentZoekObject.setZaakRelatie(gekoppeldeZaakInformatieobject.getAardRelatieWeergave().toValue());
        }
        documentZoekObject.setZaakAfgehandeld(zaak.isOpen());
        documentZoekObject.setCreatiedatum(DateTimeConverterUtil.convertToDate(informatieobject.getCreatiedatum()));
        documentZoekObject.setRegistratiedatum(DateTimeConverterUtil.convertToDate(informatieobject.getBeginRegistratie()
                .toZonedDateTime()));
        documentZoekObject.setOntvangstdatum(DateTimeConverterUtil.convertToDate(informatieobject.getOntvangstdatum()));
        documentZoekObject.setVerzenddatum(DateTimeConverterUtil.convertToDate(informatieobject.getVerzenddatum()));
        documentZoekObject.setOndertekeningDatum(DateTimeConverterUtil.convertToDate(informatieobject.getOntvangstdatum()));
        // we use the name of this enum in the search index
        documentZoekObject.setVertrouwelijkheidaanduiding(informatieobject.getVertrouwelijkheidaanduiding().name());
        documentZoekObject.setAuteur(informatieobject.getAuteur());
        if (informatieobject.getStatus() != null) {
            documentZoekObject.setStatus(informatieobject.getStatus());
        }
        documentZoekObject.setFormaat(informatieobject.getFormaat());
        documentZoekObject.setVersie(informatieobject.getVersie());
        documentZoekObject.setBestandsnaam(informatieobject.getBestandsnaam());
        documentZoekObject.setBestandsomvang(documentZoekObject.getBestandsomvang());
        documentZoekObject.setInhoudUrl(documentZoekObject.getInhoudUrl());
        documentZoekObject.setDocumentType(informatieobjecttype.getOmschrijving());
        if (informatieobject.getOndertekening() != null) {
            if (informatieobject.getOndertekening().getSoort() != null) {
                documentZoekObject.setOndertekeningSoort(informatieobject.getOndertekening().getSoort().toString());
            }
            documentZoekObject.setOndertekeningDatum(DateTimeConverterUtil.convertToDate(informatieobject.getOndertekening().getDatum()));
            documentZoekObject.setIndicatie(DocumentIndicatie.ONDERTEKEND, true);
        }
        documentZoekObject.setIndicatie(DocumentIndicatie.VERGRENDELD, informatieobject.getLocked());
        documentZoekObject.setIndicatie(DocumentIndicatie.GEBRUIKSRECHT, informatieobject.getIndicatieGebruiksrecht());
        documentZoekObject.setIndicatie(
                DocumentIndicatie.BESLUIT,
                brcClientService.isInformatieObjectGekoppeldAanBesluit(informatieobject.getUrl())
        );
        documentZoekObject.setIndicatie(DocumentIndicatie.VERZONDEN, informatieobject.getVerzenddatum() != null);
        if (informatieobject.getLocked()) {
            final EnkelvoudigInformatieObjectLock lock = enkelvoudigInformatieObjectLockService.readLock(
                    informatieobjectUUID
            );
            documentZoekObject.setVergrendeldDoorGebruikersnaam(lock.getUserId());
            documentZoekObject.setVergrendeldDoorNaam(getFullName(identityService.readUser(lock.getUserId())));
        }
        return documentZoekObject;
    }

    @Override
    public boolean supports(final ZoekObjectType objectType) {
        return objectType == ZoekObjectType.DOCUMENT;
    }
}
