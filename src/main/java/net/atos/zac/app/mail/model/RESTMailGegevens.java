/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.model;

import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding;

/**
 * REST gegevens voor het verzenden van mail
 */
public class RESTMailGegevens {

    public String verzender;

    public String ontvanger;

    public String replyTo;

    public String onderwerp;

    public String body;

    public String bijlagen;

    public boolean createDocumentFromMail;

    private RestVertrouwelijkheidaanduiding vertrouwelijkheidaanduiding;

    public RestVertrouwelijkheidaanduiding getVertrouwelijkheidaanduiding() {
        return vertrouwelijkheidaanduiding;
    }

    public void setVertrouwelijkheidaanduiding(final RestVertrouwelijkheidaanduiding vertrouwelijkheidaanduiding) {
        this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding;
    }
}
