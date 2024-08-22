package net.atos.zac.zoeken.converter;

import static net.atos.client.zgw.zrc.util.StatusTypeUtil.isHeropend;
import static net.atos.zac.identity.model.UserKt.getFullName;
import static net.atos.zac.util.UriUtil.uuidFromURI;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import net.atos.client.zgw.shared.ZGWApiService;
import net.atos.client.zgw.shared.model.Results;
import net.atos.client.zgw.zrc.ZrcClientService;
import net.atos.client.zgw.zrc.model.Geometry;
import net.atos.client.zgw.zrc.model.Rol;
import net.atos.client.zgw.zrc.model.Status;
import net.atos.client.zgw.zrc.model.Zaak;
import net.atos.client.zgw.zrc.model.generated.Resultaat;
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject;
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.ResultaatType;
import net.atos.client.zgw.ztc.model.generated.StatusType;
import net.atos.client.zgw.ztc.model.generated.ZaakType;
import net.atos.zac.flowable.FlowableTaskService;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;
import net.atos.zac.util.DateTimeConverterUtil;
import net.atos.zac.zoeken.model.ZaakIndicatie;
import net.atos.zac.zoeken.model.index.ZoekObjectType;
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject;

public class ZaakZoekObjectConverter extends AbstractZoekObjectConverter<ZaakZoekObject> {
    private final ZrcClientService zrcClientService;
    private final ZtcClientService ztcClientService;
    private final ZGWApiService zgwApiService;
    private final IdentityService identityService;
    private final FlowableTaskService flowableTaskService;

    @Inject
    public ZaakZoekObjectConverter(
            ZrcClientService zrcClientService,
            ZtcClientService ztcClientService,
            ZGWApiService zgwApiService,
            IdentityService identityService,
            FlowableTaskService flowableTaskService
    ) {
        this.zrcClientService = zrcClientService;
        this.ztcClientService = ztcClientService;
        this.zgwApiService = zgwApiService;
        this.identityService = identityService;
        this.flowableTaskService = flowableTaskService;
    }

    public ZaakZoekObject convert(final String zaakUUID) {
        final Zaak zaak = zrcClientService.readZaak(UUID.fromString(zaakUUID));
        return convert(zaak);
    }

    private ZaakZoekObject convert(final Zaak zaak) {
        final ZaakZoekObject zaakZoekObject = new ZaakZoekObject();
        zaakZoekObject.setUuid(zaak.getUuid().toString());
        zaakZoekObject.setType(ZoekObjectType.ZAAK);
        zaakZoekObject.setIdentificatie(zaak.getIdentificatie());
        zaakZoekObject.setOmschrijving(zaak.getOmschrijving());
        zaakZoekObject.setToelichting(zaak.getToelichting());
        zaakZoekObject.setRegistratiedatum(DateTimeConverterUtil.convertToDate(zaak.getRegistratiedatum()));
        zaakZoekObject.setStartdatum(DateTimeConverterUtil.convertToDate(zaak.getStartdatum()));
        zaakZoekObject.setEinddatumGepland(DateTimeConverterUtil.convertToDate(zaak.getEinddatumGepland()));
        zaakZoekObject.setEinddatum(DateTimeConverterUtil.convertToDate(zaak.getEinddatum()));
        zaakZoekObject.setUiterlijkeEinddatumAfdoening(
                DateTimeConverterUtil.convertToDate(zaak.getUiterlijkeEinddatumAfdoening())
        );
        zaakZoekObject.setPublicatiedatum(DateTimeConverterUtil.convertToDate(zaak.getPublicatiedatum()));
        // make sure to use the enum name and not the value here since the frontend translations rely on the enum name
        zaakZoekObject.setVertrouwelijkheidaanduiding(zaak.getVertrouwelijkheidaanduiding().name());
        zaakZoekObject.setAfgehandeld(!zaak.isOpen());
        zgwApiService.findInitiatorRoleForZaak(zaak).ifPresent(zaakZoekObject::setInitiator);
        zaakZoekObject.setLocatie(convertToLocatie(zaak.getZaakgeometrie()));
        zaakZoekObject.setCommunicatiekanaal(zaak.getCommunicatiekanaalNaam());

        addBetrokkenen(zaak, zaakZoekObject);

        final Group group = findGroup(zaak);
        if (group != null) {
            zaakZoekObject.setGroepID(group.getId());
            zaakZoekObject.setGroepNaam(group.getName());
        }

        final User behandelaar = findBehandelaar(zaak);
        if (behandelaar != null) {
            zaakZoekObject.setBehandelaarNaam(getFullName(behandelaar));
            zaakZoekObject.setBehandelaarGebruikersnaam(behandelaar.getId());
            zaakZoekObject.setToegekend(true);
        }

        if (zaak.isVerlengd()) {
            zaakZoekObject.setIndicatie(ZaakIndicatie.VERLENGD, true);
            zaakZoekObject.setDuurVerlenging(String.valueOf(zaak.getVerlenging().getDuur()));
            zaakZoekObject.setRedenVerlenging(zaak.getVerlenging().getReden());
        }

        if (zaak.isOpgeschort()) {
            zaakZoekObject.setRedenOpschorting(zaak.getOpschorting().getReden());
            zaakZoekObject.setIndicatie(ZaakIndicatie.OPSCHORTING, true);
        }

        if (zaak.getArchiefnominatie() != null) {
            zaakZoekObject.setArchiefNominatie(zaak.getArchiefnominatie().toString());
        }
        zaakZoekObject.setArchiefActiedatum(DateTimeConverterUtil.convertToDate(zaak.getArchiefactiedatum()));

        zaakZoekObject.setIndicatie(ZaakIndicatie.DEELZAAK, zaak.isDeelzaak());
        zaakZoekObject.setIndicatie(ZaakIndicatie.HOOFDZAAK, zaak.is_Hoofdzaak());

        final ZaakType zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype());
        zaakZoekObject.setZaaktypeIdentificatie(zaaktype.getIdentificatie());
        zaakZoekObject.setZaaktypeOmschrijving(zaaktype.getOmschrijving());
        zaakZoekObject.setZaaktypeUuid(uuidFromURI(zaaktype.getUrl()).toString());

        if (zaak.getStatus() != null) {
            final Status status = zrcClientService.readStatus(zaak.getStatus());
            zaakZoekObject.setStatusToelichting(status.getStatustoelichting());
            zaakZoekObject.setStatusDatumGezet(DateTimeConverterUtil.convertToDate(status.getDatumStatusGezet()));
            final StatusType statustype = ztcClientService.readStatustype(status.getStatustype());
            zaakZoekObject.setStatustypeOmschrijving(statustype.getOmschrijving());
            zaakZoekObject.setStatusEindstatus(statustype.getIsEindstatus());
            zaakZoekObject.setIndicatie(ZaakIndicatie.HEROPEND, isHeropend(statustype));
        }

        zaakZoekObject.setAantalOpenstaandeTaken(flowableTaskService.countOpenTasksForZaak(zaak.getUuid()));

        if (zaak.getResultaat() != null) {
            final Resultaat resultaat = zrcClientService.readResultaat(zaak.getResultaat());
            if (resultaat != null) {
                final ResultaatType resultaattype = ztcClientService.readResultaattype(resultaat.getResultaattype());
                zaakZoekObject.setResultaattypeOmschrijving(resultaattype.getOmschrijving());
                zaakZoekObject.setResultaatToelichting(resultaat.getToelichting());
            }
        }
        zaakZoekObject.setBagObjectIDs(getBagObjectIDs(zaak));

        return zaakZoekObject;
    }


    private String convertToLocatie(final Geometry zaakgeometrie) {
        //todo
        return null;
    }

    private void addBetrokkenen(final Zaak zaak, final ZaakZoekObject zaakZoekObject) {
        for (Rol<?> rol : zrcClientService.listRollen(zaak)) {
            zaakZoekObject.addBetrokkene(rol.getOmschrijving(), rol.getIdentificatienummer());
        }
    }

    private User findBehandelaar(final Zaak zaak) {
        return zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak)
                .map(behandelaar -> identityService.readUser(
                        behandelaar.getBetrokkeneIdentificatie().getIdentificatie()))
                .orElse(null);
    }


    private Group findGroup(final Zaak zaak) {
        return zgwApiService.findGroepForZaak(zaak)
                .map(groep -> identityService.readGroup(groep.getBetrokkeneIdentificatie().getIdentificatie()))
                .orElse(null);
    }


    public List<String> getBagObjectIDs(final Zaak zaak) {
        final ZaakobjectListParameters zaakobjectListParameters = new ZaakobjectListParameters();
        zaakobjectListParameters.setZaak(zaak.getUrl());
        final Results<Zaakobject> zaakobjecten = zrcClientService.listZaakobjecten(zaakobjectListParameters);
        if (zaakobjecten.getCount() > 0) {
            return zaakobjecten.getResults()
                    .stream()
                    .filter(Zaakobject::isBagObject)
                    .map(Zaakobject::getWaarde)
                    .toList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supports(final ZoekObjectType objectType) {
        return objectType == ZoekObjectType.ZAAK;
    }
}
