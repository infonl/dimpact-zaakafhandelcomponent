package net.atos.zac.formulieren.model

import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie
import java.time.ZonedDateTime

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
        validaties = "Dummy validations"
        this.formulierDefinitie = formulierDefinitie
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
        validaties = listOf<String>("Dummy validations")
    }

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
