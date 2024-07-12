/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten.model.bedrijven;

public class RestKlantenAdres {

    /** Correspondentieadres en/of bezoekadres **/
    public String type;

    public boolean afgeschermd;

    public String volledigAdres;

    public RestKlantenAdres(final String type, final boolean afgeschermd, final String volledigAdres) {
        this.type = type;
        this.afgeschermd = afgeschermd;
        this.volledigAdres = volledigAdres;
    }
}
