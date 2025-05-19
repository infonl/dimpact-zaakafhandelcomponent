/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.notities.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.zac.app.notities.model.RestNotitie
import net.atos.zac.notities.model.Notitie
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService

class NotitieConverter @Inject constructor(
    private val identityService: IdentityService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    fun toRestNotitie(notitie: Notitie): RestNotitie {
        val medewerker = identityService.readUser(notitie.gebruikersnaamMedewerker)
        return RestNotitie(
            id = notitie.id,
            zaakUUID = notitie.zaakUUID,
            tekst = notitie.tekst,
            tijdstipLaatsteWijziging = notitie.tijdstipLaatsteWijziging,
            gebruikersnaamMedewerker = notitie.gebruikersnaamMedewerker,
            voornaamAchternaamMedewerker = "${medewerker.firstName} ${medewerker.lastName}",
            bewerkenToegestaan = loggedInUserInstance.get().id == notitie.gebruikersnaamMedewerker
        )
    }
}
