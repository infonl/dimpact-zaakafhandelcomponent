/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.notities.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.zac.app.notities.model.RestNotitie
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.notities.model.Notitie

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
