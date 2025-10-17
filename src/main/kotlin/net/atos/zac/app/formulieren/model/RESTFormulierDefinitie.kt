/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.formulieren.model

import java.time.ZonedDateTime

class RESTFormulierDefinitie {
    var id: Long? = null

    var systeemnaam: String? = null

    var naam: String? = null

    var beschrijving: String? = null

    var uitleg: String? = null

    var creatiedatum: ZonedDateTime? = null

    var wijzigingsdatum: ZonedDateTime? = null

    var veldDefinities: List<RESTFormulierVeldDefinitie> = emptyList()
}
