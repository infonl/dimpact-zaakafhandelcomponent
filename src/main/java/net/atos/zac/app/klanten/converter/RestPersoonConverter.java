/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten.converter;

import static net.atos.zac.app.klanten.model.personen.RestPersonenParameters.Cardinaliteit.NON;
import static net.atos.zac.app.klanten.model.personen.RestPersonenParameters.Cardinaliteit.OPT;
import static net.atos.zac.app.klanten.model.personen.RestPersonenParameters.Cardinaliteit.REQ;
import static net.atos.zac.util.StringUtil.NON_BREAKING_SPACE;
import static net.atos.zac.util.StringUtil.ONBEKEND;
import static net.atos.zac.util.StringUtil.joinNonBlankWith;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replace;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.atos.client.brp.model.generated.AbstractDatum;
import net.atos.client.brp.model.generated.AbstractVerblijfplaats;
import net.atos.client.brp.model.generated.Adres;
import net.atos.client.brp.model.generated.AdresseringBeperkt;
import net.atos.client.brp.model.generated.DatumOnbekend;
import net.atos.client.brp.model.generated.JaarDatum;
import net.atos.client.brp.model.generated.JaarMaandDatum;
import net.atos.client.brp.model.generated.PersonenQuery;
import net.atos.client.brp.model.generated.PersonenQueryResponse;
import net.atos.client.brp.model.generated.Persoon;
import net.atos.client.brp.model.generated.PersoonBeperkt;
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummer;
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse;
import net.atos.client.brp.model.generated.VerblijfadresBinnenland;
import net.atos.client.brp.model.generated.VerblijfadresBuitenland;
import net.atos.client.brp.model.generated.VerblijfplaatsBuitenland;
import net.atos.client.brp.model.generated.VerblijfplaatsOnbekend;
import net.atos.client.brp.model.generated.VolledigeDatum;
import net.atos.client.brp.model.generated.Waardetabel;
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatum;
import net.atos.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse;
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijving;
import net.atos.client.brp.model.generated.ZoekMetNaamEnGemeenteVanInschrijvingResponse;
import net.atos.client.brp.model.generated.ZoekMetNummeraanduidingIdentificatieResponse;
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummer;
import net.atos.client.brp.model.generated.ZoekMetPostcodeEnHuisnummerResponse;
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijving;
import net.atos.client.brp.model.generated.ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse;
import net.atos.zac.app.klanten.model.personen.RestListPersonenParameters;
import net.atos.zac.app.klanten.model.personen.RestPersonenParameters;
import net.atos.zac.app.klanten.model.personen.RestPersoon;

public class RestPersoonConverter {
    // Moet overeenkomen met wat er in convertToPersonenQuery gebeurt.
    public static final List<RestPersonenParameters> VALID_PERSONEN_QUERIES = List.of(
            new RestPersonenParameters(REQ,
                    NON, NON, NON,
                    NON,
                    NON,
                    NON, NON, NON),
            new RestPersonenParameters(NON,
                    REQ, OPT, OPT,
                    REQ,
                    NON,
                    NON, NON, NON),
            new RestPersonenParameters(NON,
                    REQ, REQ, OPT,
                    NON,
                    REQ,
                    NON, NON, NON),
            new RestPersonenParameters(NON,
                    NON, NON, NON,
                    NON,
                    NON,
                    REQ, REQ, NON),
            new RestPersonenParameters(NON,
                    NON, NON, NON,
                    NON,
                    REQ,
                    NON, REQ, REQ)
    );

    public List<RestPersoon> convertPersonen(final List<Persoon> personen) {
        return personen.stream().map(this::convertPersoon).toList();
    }

    public List<RestPersoon> convertPersonenBeperkt(final List<PersoonBeperkt> personen) {
        return personen.stream().map(this::convertPersoonBeperkt).toList();
    }

    public RestPersoon convertPersoon(final Persoon persoon) {
        final RestPersoon restPersoon = new RestPersoon();
        restPersoon.bsn = persoon.getBurgerservicenummer();
        if (persoon.getGeslacht() != null) {
            restPersoon.geslacht = convertGeslacht(persoon.getGeslacht());
        }
        if (persoon.getNaam() != null) {
            restPersoon.naam = persoon.getNaam().getVolledigeNaam();
        }
        if (persoon.getGeboorte() != null) {
            restPersoon.geboortedatum = convertGeboortedatum(persoon.getGeboorte().getDatum());
        }
        if (persoon.getVerblijfplaats() != null) {
            restPersoon.verblijfplaats = convertVerblijfplaats(persoon.getVerblijfplaats());
        }
        return restPersoon;
    }

    public RestPersoon convertPersoonBeperkt(final PersoonBeperkt persoon) {
        final RestPersoon restPersoon = new RestPersoon();
        restPersoon.bsn = persoon.getBurgerservicenummer();
        if (persoon.getGeslacht() != null) {
            restPersoon.geslacht = convertGeslacht(persoon.getGeslacht());
        }
        if (persoon.getNaam() != null) {
            restPersoon.naam = persoon.getNaam().getVolledigeNaam();
        }
        if (persoon.getGeboorte() != null) {
            restPersoon.geboortedatum = convertGeboortedatum(persoon.getGeboorte().getDatum());
        }
        if (persoon.getAdressering() != null) {
            final AdresseringBeperkt adressering = persoon.getAdressering();
            restPersoon.verblijfplaats = joinNonBlankWith(", ",
                    adressering.getAdresregel1(),
                    adressering.getAdresregel2(),
                    adressering.getAdresregel3());
        }
        return restPersoon;
    }

    public PersonenQuery convertToPersonenQuery(final RestListPersonenParameters parameters) {
        if (isNotBlank(parameters.bsn)) {
            final var query = new RaadpleegMetBurgerservicenummer();
            query.addBurgerservicenummerItem(parameters.bsn);
            return query;
        }
        if (isNotBlank(parameters.geslachtsnaam) && parameters.geboortedatum != null) {
            final var query = new ZoekMetGeslachtsnaamEnGeboortedatum();
            query.setGeslachtsnaam(parameters.geslachtsnaam);
            query.setGeboortedatum(parameters.geboortedatum);
            query.setVoornamen(parameters.voornamen);
            query.setVoorvoegsel(parameters.voorvoegsel);
            return query;
        }
        if (isNotBlank(parameters.geslachtsnaam) && isNotBlank(parameters.voornamen) &&
            isNotBlank(parameters.gemeenteVanInschrijving)) {
            final var query = new ZoekMetNaamEnGemeenteVanInschrijving();
            query.setGeslachtsnaam(parameters.geslachtsnaam);
            query.setVoornamen(parameters.voornamen);
            query.setGemeenteVanInschrijving(parameters.gemeenteVanInschrijving);
            query.setVoorvoegsel(parameters.voorvoegsel);
            return query;
        }
        if (isNotBlank(parameters.postcode) && parameters.huisnummer != null) {
            final var query = new ZoekMetPostcodeEnHuisnummer();
            query.setPostcode(parameters.postcode);
            query.setHuisnummer(parameters.huisnummer);
            return query;
        }
        if (isNotBlank(parameters.straat) && parameters.huisnummer != null && isNotBlank(parameters.gemeenteVanInschrijving)) {
            final var query = new ZoekMetStraatHuisnummerEnGemeenteVanInschrijving();
            query.setStraat(parameters.straat);
            query.setHuisnummer(parameters.huisnummer);
            query.setGemeenteVanInschrijving(parameters.gemeenteVanInschrijving);
            return query;
        }
        throw new IllegalArgumentException("Ongeldige combinatie van zoek parameters");
    }

    public List<RestPersoon> convertFromPersonenQueryResponse(final PersonenQueryResponse personenQueryResponse) {
        return switch (personenQueryResponse) {
            case RaadpleegMetBurgerservicenummerResponse response -> convertPersonen(response.getPersonen());
            case ZoekMetGeslachtsnaamEnGeboortedatumResponse response -> convertPersonenBeperkt(response.getPersonen());
            case ZoekMetNaamEnGemeenteVanInschrijvingResponse response ->
                    convertPersonenBeperkt(response.getPersonen());
            case ZoekMetNummeraanduidingIdentificatieResponse response ->
                    convertPersonenBeperkt(response.getPersonen());
            case ZoekMetPostcodeEnHuisnummerResponse response -> convertPersonenBeperkt(response.getPersonen());
            case ZoekMetStraatHuisnummerEnGemeenteVanInschrijvingResponse response ->
                    convertPersonenBeperkt(response.getPersonen());
            default -> Collections.emptyList();
        };
    }

    private String convertGeslacht(final Waardetabel geslacht) {
        return isNotBlank(geslacht.getOmschrijving()) ? geslacht.getOmschrijving() : geslacht.getCode();
    }

    private String convertGeboortedatum(final AbstractDatum abstractDatum) {
        return switch (abstractDatum) {
            case VolledigeDatum volledigeDatum -> volledigeDatum.getDatum().toString();
            case JaarMaandDatum jaarMaandDatum -> "%d2-%d4".formatted(jaarMaandDatum.getMaand(),
                                                                      jaarMaandDatum.getJaar());
            case JaarDatum jaarDatum -> "%d4".formatted(jaarDatum.getJaar());
            case DatumOnbekend ignored -> ONBEKEND;
            default -> null;
        };
    }

    private String convertVerblijfplaats(final AbstractVerblijfplaats abstractVerblijfplaats) {
        return switch (abstractVerblijfplaats) {
            case Adres adres when adres.getVerblijfadres() != null ->
                    convertVerblijfadresBinnenland(adres.getVerblijfadres());
            case VerblijfplaatsBuitenland verblijfplaatsBuitenland when verblijfplaatsBuitenland.getVerblijfadres() != null ->
                    convertVerblijfadresBuitenland(verblijfplaatsBuitenland.getVerblijfadres());
            case VerblijfplaatsOnbekend ignored -> ONBEKEND;
            default -> null;
        };
    }

    private String convertVerblijfadresBinnenland(final VerblijfadresBinnenland verblijfadresBinnenland) {
        final String adres = replace(joinNonBlankWith(NON_BREAKING_SPACE,
                verblijfadresBinnenland.getOfficieleStraatnaam(),
                Objects.toString(verblijfadresBinnenland.getHuisnummer(), null),
                verblijfadresBinnenland.getHuisnummertoevoeging(),
                verblijfadresBinnenland.getHuisletter()),
                SPACE, NON_BREAKING_SPACE);
        final String postcode = replace(verblijfadresBinnenland.getPostcode(), SPACE, NON_BREAKING_SPACE);
        final String woonplaats = replace(verblijfadresBinnenland.getWoonplaats(), SPACE, NON_BREAKING_SPACE);
        return joinNonBlankWith(", ", adres, postcode, woonplaats);
    }

    private String convertVerblijfadresBuitenland(VerblijfadresBuitenland verblijfadresBuitenland) {
        return joinNonBlankWith(", ", verblijfadresBuitenland.getRegel1(),
                verblijfadresBuitenland.getRegel2(),
                verblijfadresBuitenland.getRegel3());
    }

}
