/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import net.atos.zac.admin.model.ZaakAfzender;

public class RESTReplyTo {

    public String mail;

    public boolean speciaal;

    public RESTReplyTo() {
    }

    public RESTReplyTo(ZaakAfzender.SpecialMail specialMail) {
        this.mail = specialMail.name();
        this.speciaal = true;
    }
}
