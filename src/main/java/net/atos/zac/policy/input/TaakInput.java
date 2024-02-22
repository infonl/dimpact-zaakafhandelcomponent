/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.policy.input;

import net.atos.zac.authentication.LoggedInUser;

public class TaakInput extends UserInput {

  private final TaakData taak;

  public TaakInput(final LoggedInUser loggedInUser, final TaakData taakData) {
    super(loggedInUser);
    taak = taakData;
  }

  public TaakData getTaak() {
    return taak;
  }
}
