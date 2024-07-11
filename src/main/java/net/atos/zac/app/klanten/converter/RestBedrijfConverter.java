/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten.converter;

import static net.atos.zac.util.StringUtil.NON_BREAKING_SPACE;
import static net.atos.zac.util.StringUtil.joinNonBlankWith;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.replace;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import net.atos.client.kvk.model.KvkZoekenParameters;
import net.atos.client.kvk.zoeken.model.generated.BinnenlandsAdres;
import net.atos.client.kvk.zoeken.model.generated.Resultaat;
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem;
import net.atos.zac.app.klanten.model.bedrijven.RESTBedrijf;
import net.atos.zac.app.klanten.model.bedrijven.RESTListBedrijvenParameters;

public class RestBedrijfConverter {
    public static KvkZoekenParameters convert(final RESTListBedrijvenParameters restListParameters) {
        final KvkZoekenParameters zoekenParameters = new KvkZoekenParameters();
        if (StringUtils.isNotBlank(restListParameters.kvkNummer)) {
            zoekenParameters.setKvkNummer(restListParameters.kvkNummer);
        }
        if (StringUtils.isNotBlank(restListParameters.vestigingsnummer)) {
            zoekenParameters.setVestigingsnummer(restListParameters.vestigingsnummer);
        }
        if (StringUtils.isNotBlank(restListParameters.rsin)) {
            zoekenParameters.setRsin(restListParameters.rsin);
        }
        if (StringUtils.isNotBlank(restListParameters.handelsnaam)) {
            zoekenParameters.setNaam(restListParameters.handelsnaam);
        }
        if (restListParameters.type != null) {
            zoekenParameters.setType(restListParameters.type.getType());
        }
        if (StringUtils.isNotBlank(restListParameters.postcode)) {
            zoekenParameters.setPostcode(restListParameters.postcode);
        }
        if (restListParameters.huisnummer != null) {
            zoekenParameters.setHuisnummer(String.valueOf(restListParameters.huisnummer));
        }
        return zoekenParameters;
    }

    public static Stream<RESTBedrijf> convert(final Resultaat resultaat) {
        if (CollectionUtils.isEmpty(resultaat.getResultaten())) {
            return Stream.empty();
        }
        return resultaat.getResultaten().stream().map(RestBedrijfConverter::convert);
    }

    public static RESTBedrijf convert(final ResultaatItem bedrijf) {
        final RESTBedrijf restBedrijf = new RESTBedrijf();
        restBedrijf.kvkNummer = bedrijf.getKvkNummer();
        restBedrijf.vestigingsnummer = bedrijf.getVestigingsnummer();
        restBedrijf.handelsnaam = convertToNaam(bedrijf);
        restBedrijf.postcode = bedrijf.getAdres().getBinnenlandsAdres().getPostcode();
        restBedrijf.rsin = bedrijf.getRsin();
        restBedrijf.type = bedrijf.getType().toUpperCase(Locale.getDefault());
        restBedrijf.adres = convertAdres(bedrijf);
        return restBedrijf;
    }

    private static String convertToNaam(final ResultaatItem bedrijf) {
        return replace(bedrijf.getNaam(), SPACE, NON_BREAKING_SPACE);
    }

    private static String convertAdres(final ResultaatItem bedrijf) {
        final BinnenlandsAdres binnenlandsAdres = bedrijf.getAdres().getBinnenlandsAdres();
        final String adres = replace(
                joinNonBlankWith(
                        NON_BREAKING_SPACE,
                        binnenlandsAdres.getStraatnaam(),
                        Objects.toString(binnenlandsAdres.getHuisnummer(), null),
                        binnenlandsAdres.getHuisletter()
                ),
                SPACE,
                NON_BREAKING_SPACE
        );
        final String postcode = replace(binnenlandsAdres.getPostcode(), SPACE, NON_BREAKING_SPACE);
        final String woonplaats = replace(binnenlandsAdres.getPlaats(), SPACE, NON_BREAKING_SPACE);
        return joinNonBlankWith(", ", adres, postcode, woonplaats);
    }
}
