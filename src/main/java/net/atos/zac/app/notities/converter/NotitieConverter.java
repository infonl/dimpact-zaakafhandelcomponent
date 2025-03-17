/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.notities.converter;

import java.time.ZonedDateTime;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import net.atos.zac.app.notities.model.RESTNotitie;
import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.User;
import net.atos.zac.notities.model.Notitie;
import nl.info.zac.authentication.LoggedInUser;

public class NotitieConverter {

    @Inject
    private IdentityService identityService;

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    public RESTNotitie convertToRESTNotitie(final Notitie notitie) {
        final RESTNotitie restNotitie = new RESTNotitie();
        restNotitie.id = notitie.getId();
        restNotitie.zaakUUID = notitie.getZaakUUID();
        restNotitie.tekst = notitie.getTekst();
        restNotitie.tijdstipLaatsteWijziging = notitie.getTijdstipLaatsteWijziging();
        restNotitie.gebruikersnaamMedewerker = notitie.getGebruikersnaamMedewerker();
        final User medewerker = identityService.readUser(notitie.getGebruikersnaamMedewerker());
        restNotitie.voornaamAchternaamMedewerker = String.format("%s %s", medewerker.getFirstName(), medewerker.getLastName());
        restNotitie.bewerkenToegestaan = loggedInUserInstance.get().getId().equals(notitie.getGebruikersnaamMedewerker());
        return restNotitie;
    }

    public static Notitie convertToNotitie(final RESTNotitie restNotitie) {
        final Notitie notitie = new Notitie();
        notitie.setId(restNotitie.id);
        notitie.setZaakUUID(restNotitie.zaakUUID);
        notitie.setTekst(restNotitie.tekst);
        notitie.setTijdstipLaatsteWijziging(ZonedDateTime.now());
        notitie.setGebruikersnaamMedewerker(restNotitie.gebruikersnaamMedewerker);
        return notitie;
    }
}
