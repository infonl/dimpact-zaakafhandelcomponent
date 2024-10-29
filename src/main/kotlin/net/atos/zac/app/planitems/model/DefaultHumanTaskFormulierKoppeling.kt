/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.FormulierVeldDefinitie

enum class DefaultHumanTaskFormulierKoppeling(
    private val planItemDefinitionId: String,
    val formulierDefinitie: FormulierDefinitie
) {
    AANVULLENDE_INFORMATIE("AANVULLENDE_INFORMATIE", FormulierDefinitie.AANVULLENDE_INFORMATIE),
    GOEDKEUREN("GOEDKEUREN", FormulierDefinitie.GOEDKEUREN),
    ADVIES_INTERN("ADVIES_INTERN", FormulierDefinitie.ADVIES),
    ADVIES_EXTERN("ADVIES_EXTERN", FormulierDefinitie.EXTERN_ADVIES_VASTLEGGEN),
    DOCUMENT_VERZENDEN_POST("DOCUMENT_VERZENDEN_POST", FormulierDefinitie.DOCUMENT_VERZENDEN_POST),
    DEFAULT("", FormulierDefinitie.DEFAULT_TAAKFORMULIER);

    companion object {
        @JvmStatic
        fun readFormulierDefinitie(planItemDefinitionId: String): FormulierDefinitie =
            entries.toTypedArray()
                .filter { it.planItemDefinitionId == planItemDefinitionId }
                .map { it.formulierDefinitie }
                .firstOrNull() ?: DEFAULT.formulierDefinitie

        @JvmStatic
        fun readFormulierVeldDefinities(planItemDefinitionId: String): Set<FormulierVeldDefinitie> =
            readFormulierDefinitie(planItemDefinitionId).veldDefinities
    }
}
