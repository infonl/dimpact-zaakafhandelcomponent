/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signalering.converter

import jakarta.inject.Inject
import net.atos.zac.app.signalering.model.RESTSignaleringInstellingen
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringTarget
import java.util.stream.Collectors

class RESTSignaleringInstellingenConverter @Inject constructor(
    private var signaleringService: SignaleringService
) {
    fun convert(instellingen: SignaleringInstellingen): RESTSignaleringInstellingen {
        RESTSignaleringInstellingen(
            id = instellingen.id,
            type = instellingen.type.type,
            subjecttype = instellingen.type.subjecttype
        ).apply {
            if (instellingen.type.type.isDashboard && instellingen.ownerType != SignaleringTarget.GROUP) {
                dashboard = instellingen.isDashboard
            }
            if (instellingen.type.type.isMail) {
                mail = instellingen.isMail
            }
            return this
        }
    }

    fun convert(instellingen: Collection<SignaleringInstellingen>): List<RESTSignaleringInstellingen> =
        instellingen.stream()
            .map { this.convert(it) }
            .collect(Collectors.toList())

    fun convert(restInstellingen: RESTSignaleringInstellingen, group: Group): SignaleringInstellingen =
        signaleringService.readInstellingenGroup(restInstellingen.type, group.id).let {
            it.isDashboard = false
            it.isMail = it.type.type.isMail && restInstellingen.mail!!
            return it
        }

    fun convert(restInstellingen: RESTSignaleringInstellingen, user: User): SignaleringInstellingen =
        signaleringService.readInstellingenUser(restInstellingen.type, user.id).let {
            it.isDashboard = it.type.type.isDashboard && restInstellingen.dashboard!!
            it.isMail = it.type.type.isMail && restInstellingen.mail!!
            return it
        }
}
