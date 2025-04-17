/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.converter

import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie
import net.atos.zac.formulieren.model.FormulierVeldDefinitie

private const val VALIDATIES_SEPARATOR = ";"

fun FormulierVeldDefinitie.toRESTFormulierVeldDefinitie() =
    RESTFormulierVeldDefinitie().apply {
        id = this@toRESTFormulierVeldDefinitie.id
        systeemnaam = this@toRESTFormulierVeldDefinitie.systeemnaam
        volgorde = this@toRESTFormulierVeldDefinitie.volgorde
        label = this@toRESTFormulierVeldDefinitie.label
        veldtype = this@toRESTFormulierVeldDefinitie.veldtype
        verplicht = this@toRESTFormulierVeldDefinitie.isVerplicht
        beschrijving = this@toRESTFormulierVeldDefinitie.beschrijving
        helptekst = this@toRESTFormulierVeldDefinitie.helptekst
        defaultWaarde = this@toRESTFormulierVeldDefinitie.defaultWaarde
        meerkeuzeOpties = this@toRESTFormulierVeldDefinitie.meerkeuzeOpties
        validaties = this@toRESTFormulierVeldDefinitie.validaties?.split(VALIDATIES_SEPARATOR)
    }

fun RESTFormulierVeldDefinitie.toFormulierVeldDefinitie() =
    FormulierVeldDefinitie().apply {
        id = this@toFormulierVeldDefinitie.id
        systeemnaam = this@toFormulierVeldDefinitie.systeemnaam
        volgorde = this@toFormulierVeldDefinitie.volgorde
        label = this@toFormulierVeldDefinitie.label
        veldtype = this@toFormulierVeldDefinitie.veldtype
        isVerplicht = this@toFormulierVeldDefinitie.verplicht
        beschrijving = this@toFormulierVeldDefinitie.beschrijving
        helptekst = this@toFormulierVeldDefinitie.helptekst
        defaultWaarde = this@toFormulierVeldDefinitie.defaultWaarde
        meerkeuzeOpties = this@toFormulierVeldDefinitie.meerkeuzeOpties
        validaties = this@toFormulierVeldDefinitie.validaties?.joinToString(separator = VALIDATIES_SEPARATOR)
    }
