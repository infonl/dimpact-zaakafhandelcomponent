/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.klant.model.Actor
import net.atos.client.klant.model.ExpandBetrokkene
import net.atos.client.klant.model.Klantcontact
import net.atos.zac.app.klant.model.contactmoment.RESTContactmoment
import org.apache.commons.lang3.StringUtils
import java.util.UUID

fun List<ExpandBetrokkene>.toInitiatorAsUuidStringMap(): Map<UUID, String> =
    this.filter { it.initiator }
        .associate { it.expand.hadKlantcontact.uuid to it.volledigeNaam }

fun Klantcontact.toRestContactMoment(contactToFullNameMap: Map<UUID, String>): RESTContactmoment =
    RESTContactmoment(
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
