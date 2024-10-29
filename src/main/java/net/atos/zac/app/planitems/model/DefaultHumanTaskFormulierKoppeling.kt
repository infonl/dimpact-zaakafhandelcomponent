/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import net.atos.zac.admin.model.FormulierDefinitie
import net.atos.zac.admin.model.FormulierVeldDefinitie
import java.util.Arrays

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
        fun readFormulierDefinitie(planItemDefinitionId: String): FormulierDefinitie {
            return Arrays.stream(entries.toTypedArray())
                .filter { humanTaskFormulierKoppeling: DefaultHumanTaskFormulierKoppeling -> humanTaskFormulierKoppeling.planItemDefinitionId == planItemDefinitionId }
                .map { obj: DefaultHumanTaskFormulierKoppeling -> obj.formulierDefinitie }
                .findAny()
                .orElse(DEFAULT.formulierDefinitie)
        }

        @JvmStatic
        fun readFormulierVeldDefinities(planItemDefinitionId: String): Set<FormulierVeldDefinitie> {
            return readFormulierDefinitie(planItemDefinitionId).veldDefinities
        }
    }
}
