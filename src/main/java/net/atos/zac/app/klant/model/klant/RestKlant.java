/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klant.model.klant;

public abstract class RestKlant {

    public String emailadres;

    public String telefoonnummer;

    public abstract IdentificatieType getIdentificatieType();

    public abstract String getIdentificatie();

    public abstract String getNaam();

}
