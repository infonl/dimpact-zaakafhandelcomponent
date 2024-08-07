/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant.converter;

import org.apache.commons.collections4.CollectionUtils;

import net.atos.client.kvk.vestigingsprofiel.model.generated.SBIActiviteit;
import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging;
import net.atos.zac.app.klant.model.bedrijven.RestKlantenAdres;
import net.atos.zac.app.klant.model.bedrijven.RestVestigingsprofiel;

public class RestVestigingsprofielConverter {
    public static String VESTIGINGTYPE_HOOFDVESTIGING = "HOOFDVESTIGING";
    public static String VESTIGINGTYPE_NEVENVESTIGING = "NEVENVESTIGING";

    public RestVestigingsprofiel convert(final Vestiging vestiging) {
        final RestVestigingsprofiel restVestigingsprofiel = new RestVestigingsprofiel();
        restVestigingsprofiel.kvkNummer = vestiging.getKvkNummer();
        restVestigingsprofiel.vestigingsnummer = vestiging.getVestigingsnummer();
        restVestigingsprofiel.eersteHandelsnaam = vestiging.getEersteHandelsnaam();
        restVestigingsprofiel.rsin = vestiging.getRsin();
        restVestigingsprofiel.totaalWerkzamePersonen = vestiging.getTotaalWerkzamePersonen();
        restVestigingsprofiel.deeltijdWerkzamePersonen = vestiging.getDeeltijdWerkzamePersonen();
        restVestigingsprofiel.voltijdWerkzamePersonen = vestiging.getVoltijdWerkzamePersonen();
        restVestigingsprofiel.commercieleVestiging = isIndicatie(vestiging.getIndCommercieleVestiging());

        restVestigingsprofiel.type = isIndicatie(vestiging.getIndHoofdvestiging()) ?
                VESTIGINGTYPE_HOOFDVESTIGING : VESTIGINGTYPE_NEVENVESTIGING;
        restVestigingsprofiel.sbiHoofdActiviteit = vestiging.getSbiActiviteiten()
                .stream()
                .filter(a -> isIndicatie(a.getIndHoofdactiviteit()))
                .findAny()
                .map(SBIActiviteit::getSbiOmschrijving)
                .orElse(null);

        restVestigingsprofiel.sbiActiviteiten = vestiging.getSbiActiviteiten()
                .stream()
                .filter(a -> !isIndicatie(a.getIndHoofdactiviteit()))
                .map(SBIActiviteit::getSbiOmschrijving)
                .toList();

        restVestigingsprofiel.adressen = vestiging.getAdressen()
                .stream()
                .map(adres -> new RestKlantenAdres(adres.getType(),
                        isIndicatie(adres.getIndAfgeschermd()),
                        adres.getVolledigAdres()))
                .toList();

        restVestigingsprofiel.website = CollectionUtils.emptyIfNull(vestiging.getWebsites()).stream().findFirst().orElse(null);
        return restVestigingsprofiel;
    }

    public boolean isIndicatie(String stringIndicatie) {
        if (stringIndicatie == null) {
            return false;
        }
        return switch (stringIndicatie.toLowerCase()) {
            case "ja" -> true;
            case "nee" -> false;
            default -> throw new IllegalStateException("Unexpected value: " + stringIndicatie);
        };
    }
}
