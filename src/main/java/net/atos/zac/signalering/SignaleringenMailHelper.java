/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering;

import jakarta.inject.Inject;

import net.atos.zac.identity.IdentityService;
import net.atos.zac.identity.model.Group;
import net.atos.zac.identity.model.User;
import net.atos.zac.mail.model.MailAdres;
import net.atos.zac.signalering.model.Signalering;
import net.atos.zac.signalering.model.SignaleringTarget;

public class SignaleringenMailHelper {

  @Inject private IdentityService identityService;

  public SignaleringTarget.Mail getTargetMail(final Signalering signalering) {
    switch (signalering.getTargettype()) {
      case GROUP -> {
        final Group group = identityService.readGroup(signalering.getTarget());
        if (group.getEmail() != null) {
          return new SignaleringTarget.Mail(group.getName(), group.getEmail());
        }
      }
      case USER -> {
        final User user = identityService.readUser(signalering.getTarget());
        if (user.getEmail() != null) {
          return new SignaleringTarget.Mail(user.getFullName(), user.getEmail());
        }
      }
    }
    return null;
  }

  public MailAdres formatTo(final SignaleringTarget.Mail mail) {
    return new MailAdres(mail.emailadres, mail.naam);
  }
}
