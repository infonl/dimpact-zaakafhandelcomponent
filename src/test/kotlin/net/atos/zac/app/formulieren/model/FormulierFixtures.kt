package net.atos.zac.app.formulieren.model

import net.atos.zac.formulieren.model.FormulierDefinitie
import net.atos.zac.formulieren.model.FormulierVeldDefinitie
import net.atos.zac.formulieren.model.FormulierVeldtype
import java.time.ZonedDateTime

fun createFormulierDefinitie(): FormulierDefinitie =
    FormulierDefinitie().apply {
        id = 1L
        beschrijving = "Dummy description"
        naam = "Dummy name"
        creatiedatum = ZonedDateTime.now()
        wijzigingsdatum = ZonedDateTime.now()
        uitleg = "Dummy explanation"
        systeemnaam = "Dummy system name"
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
        systeemnaam = "Dummy system name"
        this.volgorde = volgorde
        label = "Dummy label"
        veldtype = FormulierVeldtype.TEKST_VELD
        beschrijving = "Dummy description"
        helptekst = "Dummy help text"
        defaultWaarde = "Dummy value"
        meerkeuzeOpties = "Dummy multi-options"
        validaties = "Dummy validation 1;Dummy validation 2"
        this.formulierDefinitie = formulierDefinitie
    }

fun createRESTFormulierDefinitie(): RESTFormulierDefinitie =
    RESTFormulierDefinitie().apply {
        id = 1L
        systeemnaam = "Dummy system name"
        naam = "Dummy name"
        beschrijving = "Dummy description"
        uitleg = "Dummy explanation"
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
        systeemnaam = "Dummy system name"
        this.volgorde = volgorde
        label = "Dummy label"
        veldtype = FormulierVeldtype.TEKST_VELD
        beschrijving = "Dummy description"
        helptekst = "Dummy help text"
        verplicht = false
        defaultWaarde = "Dummy value"
        meerkeuzeOpties = "Dummy multi-options"
        validaties = listOf("Dummy validation 1", "Dummy validation 2")
    }
