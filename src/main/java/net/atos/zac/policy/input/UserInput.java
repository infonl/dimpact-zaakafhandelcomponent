/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.policy.input;

import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.authentication.LoggedInUser;

public class UserInput {

  @JsonbProperty("user")
  private final UserData userData = new UserData();

  public UserInput(final LoggedInUser loggedInUser) {
    userData.id = loggedInUser.getId();
    userData.rollen = loggedInUser.getRoles();
    userData.zaaktypen =
        loggedInUser.isGeautoriseerdVoorAlleZaaktypen()
            ? null
            : loggedInUser.getGeautoriseerdeZaaktypen();
  }

  public UserData getUser() {
    return userData;
  }
}
