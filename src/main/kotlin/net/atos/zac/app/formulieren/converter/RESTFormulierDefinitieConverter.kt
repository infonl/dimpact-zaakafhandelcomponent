/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.converter

import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie
import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie
import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.formulieren.model.FormulierVeldDefinitie

class RESTFormulierDefinitieConverter {
    fun convert(
        formulierDefinitie: FormulierDefinitie,
        inclusiefVelden: Boolean
    ): RESTFormulierDefinitie {
        val restFormulierDefinitie = RESTFormulierDefinitie()
        restFormulierDefinitie.id = formulierDefinitie.id
        restFormulierDefinitie.beschrijving = formulierDefinitie.beschrijving
        restFormulierDefinitie.naam = formulierDefinitie.naam
        restFormulierDefinitie.creatiedatum = formulierDefinitie.creatiedatum
        restFormulierDefinitie.wijzigingsdatum = formulierDefinitie.wijzigingsdatum
        restFormulierDefinitie.uitleg = formulierDefinitie.uitleg
        restFormulierDefinitie.systeemnaam = formulierDefinitie.systeemnaam
        if (inclusiefVelden) {
            restFormulierDefinitie.veldDefinities = formulierDefinitie.getVeldDefinities().stream()
                .sorted(Comparator.comparingInt<FormulierVeldDefinitie?>(FormulierVeldDefinitie::volgorde))
                .map<RESTFormulierVeldDefinitie?> { obj: FormulierVeldDefinitie? -> RESTFormulierVeldDefinitieConverter.convert() }
                .toList()
        }
        return restFormulierDefinitie
    }

    fun convert(restFormulierDefinitie: RESTFormulierDefinitie): FormulierDefinitie {
        return convert(restFormulierDefinitie, FormulierDefinitie())
    }

    private fun convert(
        restFormulierDefinitie: RESTFormulierDefinitie,
        formulierDefinitie: FormulierDefinitie
    ): FormulierDefinitie {
        formulierDefinitie.id = restFormulierDefinitie.id
        formulierDefinitie.naam = restFormulierDefinitie.naam
        formulierDefinitie.systeemnaam = restFormulierDefinitie.systeemnaam
        formulierDefinitie.beschrijving = restFormulierDefinitie.beschrijving
        formulierDefinitie.uitleg = restFormulierDefinitie.uitleg
        formulierDefinitie.setVeldDefinities(
            restFormulierDefinitie.veldDefinities.stream()
                .map<FormulierVeldDefinitie?> { obj: RESTFormulierVeldDefinitie? -> RESTFormulierVeldDefinitieConverter.convert() }
                .toList())
        return formulierDefinitie
    }
}
