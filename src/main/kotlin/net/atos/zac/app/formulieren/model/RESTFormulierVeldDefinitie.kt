/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.model

import net.atos.zac.formulieren.model.FormulierVeldtype

class RESTFormulierVeldDefinitie {
    var id: Long? = null

    var systeemnaam: String? = null

    var volgorde: Int = 0

    var label: String? = null

    var veldtype: FormulierVeldtype? = null

    var beschrijving: String? = null

    var helptekst: String? = null

    var verplicht: Boolean = false

    var defaultWaarde: String? = null

    var meerkeuzeOpties: String? = null

    var validaties: List<String>? = null
}
