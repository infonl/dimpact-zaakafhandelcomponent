/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.converter

import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie
import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.formulieren.model.FormulierVeldDefinitie

fun FormulierDefinitie.toRESTFormulierDefinitie(inclusiefVelden: Boolean) =
    RESTFormulierDefinitie().apply {
        id = this@toRESTFormulierDefinitie.id
        beschrijving = this@toRESTFormulierDefinitie.beschrijving
        naam = this@toRESTFormulierDefinitie.naam
        creatiedatum = this@toRESTFormulierDefinitie.creatiedatum
        wijzigingsdatum = this@toRESTFormulierDefinitie.wijzigingsdatum
        uitleg = this@toRESTFormulierDefinitie.uitleg
        systeemnaam = this@toRESTFormulierDefinitie.systeemnaam
        if (inclusiefVelden) {
            veldDefinities = this@toRESTFormulierDefinitie.getVeldDefinities()
                .sortedWith(Comparator.comparingInt<FormulierVeldDefinitie?>(FormulierVeldDefinitie::volgorde))
                .map { it.toRESTFormulierVeldDefinitie() }
        }
    }

fun RESTFormulierDefinitie.toFormulierDefinitie() =
    FormulierDefinitie().apply {
        id = this@toFormulierDefinitie.id
        naam = this@toFormulierDefinitie.naam
        systeemnaam = this@toFormulierDefinitie.systeemnaam
        beschrijving = this@toFormulierDefinitie.beschrijving
        uitleg = this@toFormulierDefinitie.uitleg
        setVeldDefinities(
            this@toFormulierDefinitie.veldDefinities.map { it.toFormulierVeldDefinitie() }
        )
    }
