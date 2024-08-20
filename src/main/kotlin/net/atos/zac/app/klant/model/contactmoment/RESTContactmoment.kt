/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.contactmoment

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@AllOpen
@NoArgConstructor
data class RESTContactmoment(
    var registratiedatum: ZonedDateTime? = null,
    var kanaal: String? = null,
    var tekst: String? = null,
    var initiatiefnemer: String? = null,
    var medewerker: String? = null
)
