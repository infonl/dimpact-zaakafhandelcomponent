/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.formulieren.converter;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie;
import net.atos.zac.formulieren.model.FormulierVeldDefinitie;

public class RESTFormulierVeldDefinitieConverter {

    private static final String VALIDATIES_SEPARATOR = ";";

    public static RESTFormulierVeldDefinitie convert(final FormulierVeldDefinitie veldDefinitie) {
        final RESTFormulierVeldDefinitie restVeldDefinitie = new RESTFormulierVeldDefinitie();
        restVeldDefinitie.id = veldDefinitie.id;
        restVeldDefinitie.systeemnaam = veldDefinitie.systeemnaam;
        restVeldDefinitie.volgorde = veldDefinitie.volgorde;
        restVeldDefinitie.label = veldDefinitie.label;
        restVeldDefinitie.veldtype = veldDefinitie.veldtype;
        restVeldDefinitie.verplicht = veldDefinitie.isVerplicht();
        restVeldDefinitie.beschrijving = veldDefinitie.beschrijving;
        restVeldDefinitie.helptekst = veldDefinitie.helptekst;
        restVeldDefinitie.defaultWaarde = veldDefinitie.defaultWaarde;
        restVeldDefinitie.meerkeuzeOpties = veldDefinitie.meerkeuzeOpties;
        if (StringUtils.isNotBlank(veldDefinitie.validaties)) {
            restVeldDefinitie.validaties = List.of(StringUtils.split(veldDefinitie.validaties, VALIDATIES_SEPARATOR));
        }
        return restVeldDefinitie;
    }

    public static FormulierVeldDefinitie convert(final RESTFormulierVeldDefinitie restVeldDefinitie) {
        final FormulierVeldDefinitie veldDefinitie = new FormulierVeldDefinitie();
        veldDefinitie.id = restVeldDefinitie.id;
        veldDefinitie.systeemnaam = restVeldDefinitie.systeemnaam;
        veldDefinitie.volgorde = restVeldDefinitie.volgorde;
        veldDefinitie.label = restVeldDefinitie.label;
        veldDefinitie.veldtype = restVeldDefinitie.veldtype;
        veldDefinitie.setVerplicht(restVeldDefinitie.verplicht);
        veldDefinitie.beschrijving = restVeldDefinitie.beschrijving;
        veldDefinitie.helptekst = restVeldDefinitie.helptekst;
        veldDefinitie.defaultWaarde = restVeldDefinitie.defaultWaarde;
        veldDefinitie.meerkeuzeOpties = restVeldDefinitie.meerkeuzeOpties;
        if (CollectionUtils.isNotEmpty(restVeldDefinitie.validaties)) {
            veldDefinitie.validaties = String.join(VALIDATIES_SEPARATOR, restVeldDefinitie.validaties);
        }
        return veldDefinitie;
    }
}
