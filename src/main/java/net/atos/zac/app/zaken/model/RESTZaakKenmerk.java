/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model;

public class RESTZaakKenmerk {

    public String kenmerk;

    public String bron;

    public RESTZaakKenmerk(final String kenmerk, final String bron) {
        this.kenmerk = kenmerk;
        this.bron = bron;
    }
}
