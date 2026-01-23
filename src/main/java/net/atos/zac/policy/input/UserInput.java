/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.input;

import java.util.Objects;

import jakarta.json.bind.annotation.JsonbProperty;

import nl.info.zac.authentication.LoggedInUser;

public class UserInput {

    @JsonbProperty("user")
    private final UserData userData = new UserData();

    public UserInput(final LoggedInUser loggedInUser) {
        Objects.requireNonNull(loggedInUser,
                "No logged in user found. Please ensure that user did not log out and its session is still active");
        userData.id = loggedInUser.getId();
        userData.rollen = loggedInUser.getRoles();
        userData.zaaktypen = loggedInUser.isAuthorisedForAllZaaktypen() ? null : loggedInUser.getGeautoriseerdeZaaktypen();
    }

    public UserData getUser() {
        return userData;
    }
}
