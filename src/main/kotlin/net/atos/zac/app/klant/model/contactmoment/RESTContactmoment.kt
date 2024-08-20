/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.contactmoment

import java.time.ZonedDateTime

data class RESTContactmoment(
    var registratiedatum: ZonedDateTime? = null,
    var kanaal: String? = null,
    var tekst: String? = null,
    var initiatiefnemer: String? = null,
    var medewerker: String? = null
)
