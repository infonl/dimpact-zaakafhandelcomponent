/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.formulieren.model

import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.formulieren.model.FormulierVeldDefinitie
import net.atos.zac.formulieren.model.FormulierVeldtype
import java.time.ZonedDateTime

fun createFormulierDefinitie(): FormulierDefinitie =
    FormulierDefinitie().apply {
        id = 1L
        beschrijving = "Fake description"
        naam = "Fake name"
        creatiedatum = ZonedDateTime.now()
        wijzigingsdatum = ZonedDateTime.now()
        uitleg = "Fake explanation"
        systeemnaam = "Fake system name"
        setVeldDefinities(
            setOf(
                createFormulierVeldDefinitie(this, 1),
                createFormulierVeldDefinitie(this, 2)
            )
        )
    }

fun createFormulierVeldDefinitie(
    formulierDefinitie: FormulierDefinitie,
    volgorde: Int
): FormulierVeldDefinitie =
    FormulierVeldDefinitie().apply {
        id = 1L
        systeemnaam = "Fake system name"
        this.volgorde = volgorde
        label = "Fake label"
        veldtype = FormulierVeldtype.TEKST_VELD
        beschrijving = "Fake description"
        helptekst = "Fake help text"
        defaultWaarde = "Fake value"
        meerkeuzeOpties = "Fake multi-options"
        validaties = "Fake validation 1;Fake validation 2"
        this.formulierDefinitie = formulierDefinitie
    }

fun createRESTFormulierDefinitie(): RESTFormulierDefinitie =
    RESTFormulierDefinitie().apply {
        id = 1L
        systeemnaam = "Fake system name"
        naam = "Fake name"
        beschrijving = "Fake description"
        uitleg = "Fake explanation"
        creatiedatum = ZonedDateTime.now()
        wijzigingsdatum = ZonedDateTime.now()
        veldDefinities = listOf(
            createRESTFormulierVeldDefinitie(1),
            createRESTFormulierVeldDefinitie(2)
        )
    }

fun createRESTFormulierVeldDefinitie(
    volgorde: Int
): RESTFormulierVeldDefinitie =
    RESTFormulierVeldDefinitie().apply {
        id = 1L
        systeemnaam = "Fake system name"
        this.volgorde = volgorde
        label = "Fake label"
        veldtype = FormulierVeldtype.TEKST_VELD
        beschrijving = "Fake description"
        helptekst = "Fake help text"
        verplicht = false
        defaultWaarde = "Fake value"
        meerkeuzeOpties = "Fake multi-options"
        validaties = listOf("Fake validation 1", "Fake validation 2")
    }
