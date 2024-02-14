/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken.model;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import net.atos.zac.app.klanten.model.klant.IdentificatieType;

public class RESTZaakBetrokkeneGegevens {

    @NotNull
    public UUID zaakUUID;

    @NotNull
    public UUID roltypeUUID;

    @NotNull
    public String roltoelichting;
    
    @NotNull
    public IdentificatieType betrokkeneIdentificatieType;

    @NotNull
    public String betrokkeneIdentificatie;
}
