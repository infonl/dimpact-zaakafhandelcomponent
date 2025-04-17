/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.converter

import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie
import net.atos.zac.formulieren.model.FormulierVeldDefinitie
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import java.lang.String
import java.util.List

object RESTFormulierVeldDefinitieConverter {
    private const val VALIDATIES_SEPARATOR = ";"

    fun convert(veldDefinitie: FormulierVeldDefinitie): RESTFormulierVeldDefinitie {
        val restVeldDefinitie = RESTFormulierVeldDefinitie()
        restVeldDefinitie.id = veldDefinitie.id
        restVeldDefinitie.systeemnaam = veldDefinitie.systeemnaam
        restVeldDefinitie.volgorde = veldDefinitie.volgorde
        restVeldDefinitie.label = veldDefinitie.label
        restVeldDefinitie.veldtype = veldDefinitie.veldtype
        restVeldDefinitie.verplicht = veldDefinitie.isVerplicht
        restVeldDefinitie.beschrijving = veldDefinitie.beschrijving
        restVeldDefinitie.helptekst = veldDefinitie.helptekst
        restVeldDefinitie.defaultWaarde = veldDefinitie.defaultWaarde
        restVeldDefinitie.meerkeuzeOpties = veldDefinitie.meerkeuzeOpties
        if (StringUtils.isNotBlank(veldDefinitie.validaties)) {
            restVeldDefinitie.validaties =
                List.of<String?>(*StringUtils.split(veldDefinitie.validaties, VALIDATIES_SEPARATOR))
        }
        return restVeldDefinitie
    }

    fun convert(restVeldDefinitie: RESTFormulierVeldDefinitie): FormulierVeldDefinitie {
        val veldDefinitie = FormulierVeldDefinitie()
        veldDefinitie.id = restVeldDefinitie.id
        veldDefinitie.systeemnaam = restVeldDefinitie.systeemnaam
        veldDefinitie.volgorde = restVeldDefinitie.volgorde
        veldDefinitie.label = restVeldDefinitie.label
        veldDefinitie.veldtype = restVeldDefinitie.veldtype
        veldDefinitie.isVerplicht = restVeldDefinitie.verplicht
        veldDefinitie.beschrijving = restVeldDefinitie.beschrijving
        veldDefinitie.helptekst = restVeldDefinitie.helptekst
        veldDefinitie.defaultWaarde = restVeldDefinitie.defaultWaarde
        veldDefinitie.meerkeuzeOpties = restVeldDefinitie.meerkeuzeOpties
        if (CollectionUtils.isNotEmpty(restVeldDefinitie.validaties)) {
            veldDefinitie.validaties = String.join(VALIDATIES_SEPARATOR, restVeldDefinitie.validaties)
        }
        return veldDefinitie
    }
}
