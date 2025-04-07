/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.signalering.converter

import jakarta.inject.Inject
import net.atos.zac.app.signalering.model.RestSignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringInstellingen
import net.atos.zac.signalering.model.SignaleringTarget
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.signalering.SignaleringService

class RestSignaleringInstellingenConverter @Inject constructor(
    private var signaleringService: SignaleringService
) {
    fun convert(instellingen: SignaleringInstellingen): RestSignaleringInstellingen {
        RestSignaleringInstellingen(
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

    fun convert(instellingen: Collection<SignaleringInstellingen>): List<RestSignaleringInstellingen> =
        instellingen.map(this::convert)

    fun convert(restInstellingen: RestSignaleringInstellingen, group: Group): SignaleringInstellingen =
        signaleringService.readInstellingenGroup(restInstellingen.type, group.id).apply {
            isDashboard = false
            isMail = this.type.type.isMail && restInstellingen.mail == true
        }

    fun convert(restInstellingen: RestSignaleringInstellingen, user: User): SignaleringInstellingen =
        signaleringService.readInstellingenUser(restInstellingen.type, user.id).apply {
            isDashboard = this.type.type.isDashboard && restInstellingen.dashboard!!
            isMail = this.type.type.isMail && restInstellingen.mail == true
        }
}
