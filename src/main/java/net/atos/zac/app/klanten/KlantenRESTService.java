/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten;

import static net.atos.zac.app.klanten.converter.RESTPersoonConverter.VALID_PERSONEN_QUERIES;
import static net.atos.zac.util.StringUtil.ONBEKEND;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
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
import net.atos.client.klanten.KlantenClientService;
import net.atos.client.klanten.model.Klant;
import net.atos.client.kvk.KvkClientService;
import net.atos.client.kvk.model.KvkZoekenParameters;
import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging;
import net.atos.client.kvk.zoeken.model.generated.Resultaat;
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem;
import net.atos.client.zgw.ztc.ZtcClientService;
import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.zac.app.klanten.converter.RESTBedrijfConverter;
import net.atos.zac.app.klanten.converter.RESTPersoonConverter;
import net.atos.zac.app.klanten.converter.RESTRoltypeConverter;
import net.atos.zac.app.klanten.converter.RESTVestigingsprofielConverter;
import net.atos.zac.app.klanten.model.bedrijven.RESTBedrijf;
import net.atos.zac.app.klanten.model.bedrijven.RESTListBedrijvenParameters;
import net.atos.zac.app.klanten.model.bedrijven.RESTVestigingsprofiel;
import net.atos.zac.app.klanten.model.klant.IdentificatieType;
import net.atos.zac.app.klanten.model.klant.RESTContactGegevens;
import net.atos.zac.app.klanten.model.klant.RESTKlant;
import net.atos.zac.app.klanten.model.klant.RESTRoltype;
import net.atos.zac.app.klanten.model.personen.RESTListPersonenParameters;
import net.atos.zac.app.klanten.model.personen.RESTPersonenParameters;
import net.atos.zac.app.klanten.model.personen.RESTPersoon;
import net.atos.zac.app.shared.RESTResultaat;

@Path("klanten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class KlantenRESTService {
    public static final Set<RolType.OmschrijvingGeneriekEnum> betrokkenen;
    private static final RESTPersoon ONBEKEND_PERSOON = new RESTPersoon(ONBEKEND, ONBEKEND, ONBEKEND);
    static {
        betrokkenen = EnumSet.allOf(RolType.OmschrijvingGeneriekEnum.class);
        betrokkenen.remove(RolType.OmschrijvingGeneriekEnum.INITIATOR);
        betrokkenen.remove(RolType.OmschrijvingGeneriekEnum.BEHANDELAAR);
    }

    private BRPClientService brpClientService;
    private KvkClientService kvkClientService;
    private ZtcClientService ztcClientService;
    private RESTPersoonConverter restPersoonConverter;
    private RESTBedrijfConverter restBedrijfConverter;
    private RESTVestigingsprofielConverter restVestigingsprofielConverter;
    private RESTRoltypeConverter restRoltypeConverter;
    private KlantenClientService klantenClientService;

    /**
     * Default no-arg constructor, required by Weld.
     */
    public KlantenRESTService() {
    }

    @Inject
    public KlantenRESTService(
            BRPClientService brpClientService,
            KvkClientService kvkClientService,
            ZtcClientService ztcClientService,
            RESTPersoonConverter restPersoonConverter,
            RESTBedrijfConverter restBedrijfConverter,
            RESTVestigingsprofielConverter restVestigingsprofielConverter,
            RESTRoltypeConverter restRoltypeConverter,
            KlantenClientService klantenClientService
    ) {
        this.brpClientService = brpClientService;
        this.kvkClientService = kvkClientService;
        this.ztcClientService = ztcClientService;
        this.restPersoonConverter = restPersoonConverter;
        this.restBedrijfConverter = restBedrijfConverter;
        this.restVestigingsprofielConverter = restVestigingsprofielConverter;
        this.restRoltypeConverter = restRoltypeConverter;
        this.klantenClientService = klantenClientService;
    }

    @GET
    @Path("persoon/{bsn}")
    public RESTPersoon readPersoon(@PathParam("bsn") final String bsn) throws ExecutionException, InterruptedException {
        return brpClientService.findPersoonAsync(bsn)
                .thenCombine(klantenClientService.findPersoonAsync(bsn), this::convertToRESTPersoon)
                .toCompletableFuture()
                .get();
    }

    @GET
    @Path("vestiging/{vestigingsnummer}")
    public RESTBedrijf readVestiging(
            @PathParam("vestigingsnummer") final String vestigingsnummer
    )
      throws ExecutionException,
      InterruptedException {
        return kvkClientService.findVestigingAsync(vestigingsnummer)
                .thenCombine(klantenClientService.findVestigingAsync(vestigingsnummer),
                        this::convertToRESTBedrijf)
                .toCompletableFuture()
                .get();
    }

    @GET
    @Path("vestigingsprofiel/{vestigingsnummer}")
    public RESTVestigingsprofiel readVestigingsprofiel(@PathParam("vestigingsnummer") final String vestigingsnummer) {
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
    public RESTBedrijf readRechtspersoon(@PathParam("rsin") final String rsin) {
        return kvkClientService.findRechtspersoon(rsin)
                .map(restBedrijfConverter::convert)
                .orElseGet(RESTBedrijf::new);
    }

    @GET
    @Path("personen/parameters")
    public List<RESTPersonenParameters> getPersonenParameters() {
        return VALID_PERSONEN_QUERIES;
    }

    @PUT
    @Path("personen")
    public RESTResultaat<RESTPersoon> listPersonen(final RESTListPersonenParameters restListPersonenParameters) {
        final PersonenQuery query = restPersoonConverter.convertToPersonenQuery(restListPersonenParameters);
        final PersonenQueryResponse response = brpClientService.queryPersonen(query);
        return new RESTResultaat<>(restPersoonConverter.convertFromPersonenQueryResponse(response));
    }

    @PUT
    @Path("bedrijven")
    public RESTResultaat<RESTBedrijf> listBedrijven(final RESTListBedrijvenParameters restParameters) {
        final KvkZoekenParameters zoekenParameters = restBedrijfConverter.convert(restParameters);
        final Resultaat resultaat = kvkClientService.list(zoekenParameters);
        return new RESTResultaat<>(resultaat.getResultaten().stream()
                .filter(KlantenRESTService::isKoppelbaar)
                .map(restBedrijfConverter::convert)
                .toList());
    }

    @GET
    @Path("roltype/{zaaktypeUuid}/betrokkene")
    public List<RESTRoltype> listBetrokkeneRoltypen(@PathParam("zaaktypeUuid") final UUID zaaktype) {
        return restRoltypeConverter.convert(
                ztcClientService.listRoltypen(ztcClientService.readZaaktype(zaaktype).getUrl())
                        .stream()
                        .filter(roltype -> betrokkenen.contains(roltype.getOmschrijvingGeneriek())
                        ).sorted(Comparator.comparing(RolType::getOmschrijving))
        );
    }

    @GET
    @Path("roltype")
    public List<RESTRoltype> listRoltypen() {
        return restRoltypeConverter.convert(
                ztcClientService.listRoltypen()
                        .stream()
                        .sorted(Comparator.comparing(RolType::getOmschrijving))
        );
    }

    @GET
    @Path("contactgegevens/{identificatieType}/{initiatorIdentificatie}")
    public RESTContactGegevens ophalenContactGegevens(
            @PathParam("identificatieType") final IdentificatieType identificatieType,
            @PathParam("initiatorIdentificatie") final String initiatorIdentificatie
    ) {
        final RESTContactGegevens restContactGegevens = new RESTContactGegevens();
        if (identificatieType == null) {
            return restContactGegevens;
        }

        final Optional<Klant> klantOptional;
        switch (identificatieType) {
            case VN -> klantOptional = klantenClientService.findVestiging(initiatorIdentificatie);
            case BSN -> klantOptional = klantenClientService.findPersoon(initiatorIdentificatie);
            default -> klantOptional = Optional.empty();
        }

        klantOptional.ifPresent(klant -> {
            restContactGegevens.telefoonnummer = klant.getTelefoonnummer();
            restContactGegevens.emailadres = klant.getEmailadres();
        });

        return restContactGegevens;
    }

    private RESTKlant addKlantData(final RESTKlant restKlant, final Optional<Klant> klantOptional) {
        klantOptional.ifPresent(klant -> {
            restKlant.telefoonnummer = klant.getTelefoonnummer();
            restKlant.emailadres = klant.getEmailadres();
        });
        return restKlant;
    }

    private RESTBedrijf convertToRESTBedrijf(final Optional<ResultaatItem> vestiging, final Optional<Klant> klant) {
        return vestiging
                .map(restBedrijfConverter::convert)
                .map(restBedrijf -> (RESTBedrijf) addKlantData(restBedrijf, klant))
                .orElseGet(RESTBedrijf::new);
    }


    private RESTPersoon convertToRESTPersoon(final Optional<Persoon> persoon, final Optional<Klant> klant) {
        return persoon
                .map(restPersoonConverter::convertPersoon)
                .map(restPersoon -> (RESTPersoon) addKlantData(restPersoon, klant))
                .orElse(ONBEKEND_PERSOON);
    }

    private static boolean isKoppelbaar(final ResultaatItem item) {
        return item.getVestigingsnummer() != null || item.getRsin() != null;
    }
}
