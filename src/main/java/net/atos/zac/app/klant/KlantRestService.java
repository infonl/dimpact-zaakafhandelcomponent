/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant;

import static net.atos.zac.app.klant.converter.RestPersoonConverter.VALID_PERSONEN_QUERIES;
import static net.atos.zac.util.StringUtil.ONBEKEND;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.atos.client.brp.BRPClientService;
import net.atos.client.brp.model.generated.PersonenQuery;
import net.atos.client.brp.model.generated.PersonenQueryResponse;
import net.atos.client.brp.model.generated.Persoon;
import net.atos.client.klant.KlantClientService;
import net.atos.client.klant.model.DigitaalAdres;
import net.atos.client.klant.model.ExpandBetrokkene;
import net.atos.client.klant.model.ExpandBetrokkeneAllOfExpand;
import net.atos.client.kvk.KvkClientService;
import net.atos.client.kvk.model.KvkZoekenParameters;
import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging;
import net.atos.client.kvk.zoeken.model.generated.Resultaat;
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.zac.app.klant.converter.KlantcontactConverter;
import net.atos.zac.app.klant.converter.RestBedrijfConverter;
import net.atos.zac.app.klant.converter.RestPersoonConverter;
import net.atos.zac.app.klant.converter.RestRoltypeConverter;
import net.atos.zac.app.klant.converter.RestVestigingsprofielConverter;
import net.atos.zac.app.klant.model.bedrijven.RestBedrijf;
import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters;
import net.atos.zac.app.klant.model.bedrijven.RestVestigingsprofiel;
import net.atos.zac.app.klant.model.contactmoment.RESTContactmoment;
import net.atos.zac.app.klant.model.contactmoment.RESTListContactmomentenParameters;
import net.atos.zac.app.klant.model.klant.IdentificatieType;
import net.atos.zac.app.klant.model.klant.RestContactGegevens;
import net.atos.zac.app.klant.model.klant.RestKlant;
import net.atos.zac.app.klant.model.klant.RestRoltype;
import net.atos.zac.app.klant.model.personen.RestListPersonenParameters;
import net.atos.zac.app.klant.model.personen.RestPersonenParameters;
import net.atos.zac.app.klant.model.personen.RestPersoon;
import net.atos.zac.app.shared.RESTResultaat;


@Path("klanten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class KlantRestService {
    public static final Set<OmschrijvingGeneriekEnum> betrokkenen;
    private static final RestPersoon ONBEKEND_PERSOON = new RestPersoon(ONBEKEND, ONBEKEND, ONBEKEND);
    static {
        betrokkenen = EnumSet.allOf(OmschrijvingGeneriekEnum.class);
        betrokkenen.remove(OmschrijvingGeneriekEnum.INITIATOR);
        betrokkenen.remove(OmschrijvingGeneriekEnum.BEHANDELAAR);
    }
    public static final String TELEFOON_SOORT_DIGITAAL_ADRES = "telefoon";
    public static final String EMAIL_SOORT_DIGITAAL_ADRES = "email";

    private BRPClientService brpClientService;
    private KvkClientService kvkClientService;
    private ZtcClientService ztcClientService;
    private RestPersoonConverter restPersoonConverter;
    private RestVestigingsprofielConverter restVestigingsprofielConverter;
    private KlantClientService klantClientService;
    private KlantcontactConverter klantcontactConverter;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public KlantRestService() {
    }

    @Inject
    public KlantRestService(
            BRPClientService brpClientService,
            KvkClientService kvkClientService,
            ZtcClientService ztcClientService,
            RestPersoonConverter restPersoonConverter,
            RestVestigingsprofielConverter restVestigingsprofielConverter,
            KlantClientService klantClientService,
            KlantcontactConverter klantcontactConverter
    ) {
        this.brpClientService = brpClientService;
        this.kvkClientService = kvkClientService;
        this.ztcClientService = ztcClientService;
        this.restPersoonConverter = restPersoonConverter;
        this.restVestigingsprofielConverter = restVestigingsprofielConverter;
        this.klantClientService = klantClientService;
        this.klantcontactConverter = klantcontactConverter;
    }

    @GET
    @Path("persoon/{bsn}")
    public RestPersoon readPersoon(@PathParam("bsn") final String bsn) throws ExecutionException, InterruptedException {
        return convertToRestPersoon(
                brpClientService.findPersoonAsync(bsn).toCompletableFuture().get(),
                convertToRestPersoon(klantClientService.findDigitalAddressesByNumber(bsn))
        );
    }

    @GET
    @Path("vestiging/{vestigingsnummer}")
    public RestBedrijf readVestiging(
            @PathParam("vestigingsnummer") final String vestigingsnummer
    ) throws ExecutionException, InterruptedException {
        return convertToRestBedrijf(
                kvkClientService.findVestigingAsync(vestigingsnummer).toCompletableFuture().get(),
                convertToRestPersoon(klantClientService.findDigitalAddressesByNumber(vestigingsnummer))
        );
    }

    @GET
    @Path("vestigingsprofiel/{vestigingsnummer}")
    public RestVestigingsprofiel readVestigingsprofiel(@PathParam("vestigingsnummer") final String vestigingsnummer) {
        Optional<Vestiging> vestiging = kvkClientService.findVestigingsprofiel(vestigingsnummer);
        if (vestiging.isPresent()) {
            return restVestigingsprofielConverter.convert(vestiging.get());
        }
        throw new NotFoundException(
                "Geen vestigingsprofiel gevonden voor vestiging met vestigingsnummer \"%s\"".formatted(vestigingsnummer)
        );
    }

    @GET
    @Path("rechtspersoon/{rsin}")
    public RestBedrijf readRechtspersoon(@PathParam("rsin") final String rsin) {
        return kvkClientService.findRechtspersoon(rsin)
                .map(RestBedrijfConverter::convert)
                .orElseGet(RestBedrijf::new);
    }

    @GET
    @Path("personen/parameters")
    public List<RestPersonenParameters> getPersonenParameters() {
        return VALID_PERSONEN_QUERIES;
    }

    @PUT
    @Path("personen")
    public RESTResultaat<RestPersoon> listPersonen(final RestListPersonenParameters restListPersonenParameters) {
        final PersonenQuery query = restPersoonConverter.convertToPersonenQuery(restListPersonenParameters);
        final PersonenQueryResponse response = brpClientService.queryPersonen(query);
        return new RESTResultaat<>(restPersoonConverter.convertFromPersonenQueryResponse(response));
    }

    @PUT
    @Path("bedrijven")
    public RESTResultaat<RestBedrijf> listBedrijven(final RestListBedrijvenParameters restParameters) {
        final KvkZoekenParameters zoekenParameters = RestBedrijfConverter.convert(restParameters);
        final Resultaat resultaat = kvkClientService.list(zoekenParameters);
        return new RESTResultaat<>(resultaat.getResultaten().stream()
                .filter(KlantRestService::isKoppelbaar)
                .map(RestBedrijfConverter::convert)
                .toList());
    }

    @GET
    @Path("roltype/{zaaktypeUuid}/betrokkene")
    public List<RestRoltype> listBetrokkeneRoltypen(@PathParam("zaaktypeUuid") final UUID zaaktype) {
        return RestRoltypeConverter.convert(
                ztcClientService.listRoltypen(ztcClientService.readZaaktype(zaaktype).getUrl())
                        .stream()
                        .filter(roltype -> betrokkenen.contains(roltype.getOmschrijvingGeneriek()))
                        .sorted(Comparator.comparing(RolType::getOmschrijving))
        );
    }

    @GET
    @Path("roltype")
    public List<RestRoltype> listRoltypen() {
        return RestRoltypeConverter.convert(
                ztcClientService.listRoltypen()
                        .stream()
                        .sorted(Comparator.comparing(RolType::getOmschrijving))
        );
    }

    @GET
    @Path("contactgegevens/{identificatieType}/{initiatorIdentificatie}")
    public RestContactGegevens ophalenContactGegevens(
            @PathParam("identificatieType") final IdentificatieType identificatieType,
            @PathParam("initiatorIdentificatie") final String initiatorIdentificatie
    ) {
        final RestContactGegevens restContactGegevens = new RestContactGegevens();
        if (identificatieType == null) {
            return restContactGegevens;
        }

        var klantPersoon = convertToRestPersoon(
                klantClientService.findDigitalAddressesByNumber(initiatorIdentificatie)
        );
        restContactGegevens.telefoonnummer = klantPersoon.telefoonnummer;
        restContactGegevens.emailadres = klantPersoon.emailadres;

        return restContactGegevens;
    }

    private RestKlant addKlantData(final RestKlant restKlant, final RestPersoon klantPerson) {
        restKlant.telefoonnummer = klantPerson.telefoonnummer;
        restKlant.emailadres = klantPerson.emailadres;
        return restKlant;
    }

    private RestBedrijf convertToRestBedrijf(final Optional<ResultaatItem> vestiging, final RestPersoon klantPerson) {
        return vestiging
                .map(RestBedrijfConverter::convert)
                .map(restBedrijf -> (RestBedrijf) addKlantData(restBedrijf, klantPerson))
                .orElseGet(RestBedrijf::new);
    }


    private RestPersoon convertToRestPersoon(final Optional<Persoon> persoon, RestPersoon klantPersoon) {
        return persoon
                .map(restPersoonConverter::convertPersoon)
                .map(restPersoon -> (RestPersoon) addKlantData(restPersoon, klantPersoon))
                .orElse(ONBEKEND_PERSOON);
    }

    private RestPersoon convertToRestPersoon(List<DigitaalAdres> digitalAddresses) {
        RestPersoon restPersoon = new RestPersoon();
        if (digitalAddresses != null) {
            for (DigitaalAdres digitalAdress : digitalAddresses) {
                switch (digitalAdress.getSoortDigitaalAdres()) {
                    case TELEFOON_SOORT_DIGITAAL_ADRES -> restPersoon.telefoonnummer = digitalAdress.getAdres();
                    case EMAIL_SOORT_DIGITAAL_ADRES -> restPersoon.emailadres = digitalAdress.getAdres();
                }
            }
        }
        return restPersoon;
    }

    private static boolean isKoppelbaar(final ResultaatItem item) {
        return item.getVestigingsnummer() != null || item.getRsin() != null;
    }

    @PUT
    @Path("contactmomenten")
    public RESTResultaat<RESTContactmoment> listContactmomenten(final RESTListContactmomentenParameters parameters) {
        var nummer = parameters.bsn != null ? parameters.bsn : parameters.vestigingsnummer;
        // OpenKlant 2.1 pages start from 1 (not 0-based). Page 0 is considered invalid number
        var pageNumber = parameters.page + 1;

        var betrokkenenWithKlantcontactList = klantClientService.listBetrokkenenByNumber(nummer, pageNumber);

        var contactToFullNameMap = klantcontactConverter.mapContactToInitiatorFullName(betrokkenenWithKlantcontactList);

        var klantcontactListPage = betrokkenenWithKlantcontactList.stream()
                .map(ExpandBetrokkene::getExpand)
                .filter(Objects::nonNull)
                .map(ExpandBetrokkeneAllOfExpand::getHadKlantcontact)
                .map(klantcontact -> klantcontactConverter.convert(klantcontact, contactToFullNameMap))
                .toList();

        return new RESTResultaat<>(klantcontactListPage, klantcontactListPage.size());
    }
}
