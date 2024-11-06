package net.atos.zac.app.planitems.converter

import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.FormulierVeldDefinitie
import net.atos.zac.app.planitems.model.DefaultHumanTaskFormulierKoppeling.DEFAULT
import net.atos.zac.app.planitems.model.DefaultHumanTaskFormulierKoppeling.entries

fun String.toFormulierDefinitie(): FormulierDefinitie =
    entries.toTypedArray()
        .filter { it.planItemDefinitionId == this }
        .map { it.formulierDefinitie }
        .firstOrNull() ?: DEFAULT.formulierDefinitie

fun String.readFormulierVeldDefinities(): Set<FormulierVeldDefinitie> =
    this.toFormulierDefinitie().veldDefinities
