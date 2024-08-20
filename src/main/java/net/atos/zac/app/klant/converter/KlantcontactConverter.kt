/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.klant.model.Actor
import net.atos.client.klant.model.ExpandBetrokkene
import net.atos.client.klant.model.Klantcontact
import net.atos.zac.app.klant.model.contactmoment.RESTContactmoment
import org.apache.commons.lang3.StringUtils
import java.util.UUID

fun mapContactToInitiatorFullName(betrokkenenWithKlantcontactList: List<ExpandBetrokkene>): Map<UUID, String> =
    betrokkenenWithKlantcontactList
        .filter { it.initiator }
        .associate { it.expand.hadKlantcontact.uuid to it.volledigeNaam }

fun Klantcontact.toRestContactMoment(contactToFullNameMap: Map<UUID, String>): RESTContactmoment {
    val restContactmoment = RESTContactmoment()
    if (this.plaatsgevondenOp != null) {
        restContactmoment.registratiedatum = this.plaatsgevondenOp.toZonedDateTime()
    }
    restContactmoment.initiatiefnemer = contactToFullNameMap[this.uuid]
    restContactmoment.kanaal = this.kanaal
    restContactmoment.tekst = this.onderwerp
    if (this.hadBetrokkenActoren != null) {
        restContactmoment.medewerker = toRestRoltype(this.hadBetrokkenActoren)
    }
    return restContactmoment
}

private fun toRestRoltype(actors: List<Actor>): String? {
    if (actors.isEmpty()) {
        return null
    }

    val result = StringBuilder()
    for (actor in actors) {
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
