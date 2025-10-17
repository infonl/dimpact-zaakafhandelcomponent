/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.planitems.converter

import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.FormulierVeldDefinitie
import nl.info.zac.app.planitems.model.DefaultHumanTaskFormulierKoppeling.DEFAULT
import nl.info.zac.app.planitems.model.DefaultHumanTaskFormulierKoppeling.entries

fun String.toFormulierDefinitie(): FormulierDefinitie =
    entries.toTypedArray()
        .filter { it.planItemDefinitionId == this }
        .map { it.formulierDefinitie }
        .firstOrNull() ?: DEFAULT.formulierDefinitie

fun String.readFormulierVeldDefinities(): Set<FormulierVeldDefinitie> =
    this.toFormulierDefinitie().veldDefinities
