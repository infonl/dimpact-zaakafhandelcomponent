/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.converter;

import jakarta.inject.Inject;

import net.atos.zac.app.mail.model.RESTMailGegevens;
import net.atos.zac.mailtemplates.model.MailGegevens;
import nl.info.zac.configuratie.ConfiguratieService;
import nl.info.zac.mail.model.MailAdres;

public class RESTMailGegevensConverter {

    @Inject
    private ConfiguratieService configuratieService;

    public MailGegevens convert(final RESTMailGegevens restMailGegevens) {
        // Note that most of the actual conversion happens in the constructor.
        // Please do not move it here, because MailGegevens do not always get constructed here.
        final String afzender = configuratieService.readGemeenteNaam();
        return new MailGegevens(
                new MailAdres(restMailGegevens.verzender, afzender),
                new MailAdres(restMailGegevens.ontvanger, null),
                restMailGegevens.replyTo == null ? null : new MailAdres(restMailGegevens.replyTo, afzender),
                restMailGegevens.onderwerp,
                restMailGegevens.body,
                restMailGegevens.bijlagen,
                restMailGegevens.createDocumentFromMail);
    }
}
