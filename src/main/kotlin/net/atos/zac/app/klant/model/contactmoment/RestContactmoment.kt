/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.contactmoment

import net.atos.client.klant.model.Actor
import net.atos.client.klant.model.Klantcontact
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import java.time.ZonedDateTime
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestContactmoment(
    var registratiedatum: ZonedDateTime? = null,
    var kanaal: String? = null,
    var tekst: String? = null,
    var initiatiefnemer: String? = null,
    var medewerker: String? = null
)

fun Klantcontact.toRestContactMoment(contactToFullNameMap: Map<UUID, String>): RestContactmoment =
    RestContactmoment(
        registratiedatum = this.plaatsgevondenOp?.toZonedDateTime(),
        initiatiefnemer = contactToFullNameMap[this.uuid],
        kanaal = this.kanaal,
        tekst = this.onderwerp,
        medewerker = this.hadBetrokkenActoren?.toCommaSeparatedString()
    )

private fun List<Actor>.toCommaSeparatedString(): String? {
    if (this.isEmpty()) {
        return null
    }
    val result = StringBuilder()
    for (actor in this) {
        if (StringUtils.isNotBlank(result)) {
            result.append(",")
        }
        if (StringUtils.isNotBlank(actor.naam)) {
            result.append(actor.naam)
        } else {
            val actoridentificator = actor.actoridentificator
            result.append(
                "${actoridentificator.codeObjecttype} ${actoridentificator.codeRegister} " +
                    "%${actoridentificator.objectId}"
            )
        }
    }
    return result.toString()
}
