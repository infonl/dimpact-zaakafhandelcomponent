/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.formulieren.converter;

import java.util.Comparator;

import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie;
import net.atos.zac.formulieren.model.FormulierDefinitie;
import net.atos.zac.formulieren.model.FormulierVeldDefinitie;

public class RESTFormulierDefinitieConverter {
    public RESTFormulierDefinitie convert(
            final FormulierDefinitie formulierDefinitie,
            boolean inclusiefVelden
    ) {
        final RESTFormulierDefinitie restFormulierDefinitie = new RESTFormulierDefinitie();
        restFormulierDefinitie.id = formulierDefinitie.getId();
        restFormulierDefinitie.beschrijving = formulierDefinitie.getBeschrijving();
        restFormulierDefinitie.naam = formulierDefinitie.getNaam();
        restFormulierDefinitie.creatiedatum = formulierDefinitie.getCreatiedatum();
        restFormulierDefinitie.wijzigingsdatum = formulierDefinitie.getWijzigingsdatum();
        restFormulierDefinitie.uitleg = formulierDefinitie.getUitleg();
        restFormulierDefinitie.systeemnaam = formulierDefinitie.getSysteemnaam();
        if (inclusiefVelden) {
            restFormulierDefinitie.veldDefinities = formulierDefinitie.getVeldDefinities().stream()
                    .sorted(Comparator.comparingInt(FormulierVeldDefinitie::getVolgorde))
                    .map(RESTFormulierVeldDefinitieConverter::convert)
                    .toList();
        }
        return restFormulierDefinitie;
    }

    public FormulierDefinitie convert(final RESTFormulierDefinitie restFormulierDefinitie) {
        return convert(restFormulierDefinitie, new FormulierDefinitie());
    }

    private FormulierDefinitie convert(
            final RESTFormulierDefinitie restFormulierDefinitie,
            final FormulierDefinitie formulierDefinitie
    ) {
        formulierDefinitie.setId(restFormulierDefinitie.id);
        formulierDefinitie.setNaam(restFormulierDefinitie.naam);
        formulierDefinitie.setSysteemnaam(restFormulierDefinitie.systeemnaam);
        formulierDefinitie.setBeschrijving(restFormulierDefinitie.beschrijving);
        formulierDefinitie.setUitleg(restFormulierDefinitie.uitleg);
        formulierDefinitie.setVeldDefinities(restFormulierDefinitie.veldDefinities.stream()
                .map(RESTFormulierVeldDefinitieConverter::convert)
                .toList());
        return formulierDefinitie;
    }
}
