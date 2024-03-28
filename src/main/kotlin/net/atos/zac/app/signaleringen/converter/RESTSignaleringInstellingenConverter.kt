/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signaleringen.converter

import jakarta.inject.Inject
import net.atos.zac.app.signaleringen.model.RESTSignaleringInstellingen
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.signalering.SignaleringenService
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringTarget
import java.util.stream.Collectors

class RESTSignaleringInstellingenConverter @Inject constructor(
    private var signaleringenService: SignaleringenService
) {
    fun convert(instellingen: SignaleringInstellingen): RESTSignaleringInstellingen {
        val restInstellingen = RESTSignaleringInstellingen()
        restInstellingen.id = instellingen.id
        restInstellingen.type = instellingen.type.type
        restInstellingen.subjecttype = instellingen.type.subjecttype
        if (instellingen.type.type.isDashboard && instellingen.ownerType != SignaleringTarget.GROUP) {
            restInstellingen.dashboard = instellingen.isDashboard
        }
        if (instellingen.type.type.isMail) {
            restInstellingen.mail = instellingen.isMail
        }
        return restInstellingen
    }

    fun convert(instellingen: Collection<SignaleringInstellingen>): List<RESTSignaleringInstellingen> {
        return instellingen.stream()
            .map { signaleringInstellingen: SignaleringInstellingen -> this.convert(signaleringInstellingen) }
            .collect(Collectors.toList())
    }

    fun convert(restInstellingen: RESTSignaleringInstellingen, group: Group): SignaleringInstellingen {
        val instellingen = signaleringenService.readInstellingenGroup(restInstellingen.type, group.id)
        instellingen.isDashboard = false
        instellingen.isMail = instellingen.type.type.isMail && restInstellingen.mail!!
        return instellingen
    }

    fun convert(restInstellingen: RESTSignaleringInstellingen, user: User): SignaleringInstellingen {
        val instellingen = signaleringenService.readInstellingenUser(restInstellingen.type, user.id)
        instellingen.isDashboard = instellingen.type.type.isDashboard && restInstellingen.dashboard!!
        instellingen.isMail = instellingen.type.type.isMail && restInstellingen.mail!!
        return instellingen
    }
}
