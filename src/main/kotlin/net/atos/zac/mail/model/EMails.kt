/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.mail.model;

import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

public class EMails {

    @JsonbProperty("Messages")
    private List<EMail> eMails;

    public EMails(final List<EMail> eMails) {
        this.eMails = eMails;
    }

    public List<EMail> geteMails() {
        return eMails;
    }

    public void seteMails(final List<EMail> eMails) {
        this.eMails = eMails;
    }
}
