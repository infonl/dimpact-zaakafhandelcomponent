/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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
    fun convert(instellingen: SignaleringInstellingen): RESTSignaleringInstellingen =
        RESTSignaleringInstellingen().let {
            it.id = instellingen.id
            it.type = instellingen.type.type
            it.subjecttype = instellingen.type.subjecttype
            if (instellingen.type.type.isDashboard && instellingen.ownerType != SignaleringTarget.GROUP) {
                it.dashboard = instellingen.isDashboard
            }
            if (instellingen.type.type.isMail) {
                it.mail = instellingen.isMail
            }
            return it
        }

    fun convert(instellingen: Collection<SignaleringInstellingen>): List<RESTSignaleringInstellingen> =
        instellingen.stream()
            .map { this.convert(it) }
            .collect(Collectors.toList())

    fun convert(restInstellingen: RESTSignaleringInstellingen, group: Group): SignaleringInstellingen =
        signaleringenService.readInstellingenGroup(restInstellingen.type, group.id).let {
            it.isDashboard = false
            it.isMail = it.type.type.isMail && restInstellingen.mail!!
            return it
        }

    fun convert(restInstellingen: RESTSignaleringInstellingen, user: User): SignaleringInstellingen =
        signaleringenService.readInstellingenUser(restInstellingen.type, user.id).let {
            it.isDashboard = it.type.type.isDashboard && restInstellingen.dashboard!!
            it.isMail = it.type.type.isMail && restInstellingen.mail!!
            return it
        }
}
