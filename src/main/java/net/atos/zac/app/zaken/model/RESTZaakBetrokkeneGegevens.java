/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zaken.model;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

import net.atos.zac.app.klanten.model.klant.IdentificatieType;

public class RESTZaakBetrokkeneGegevens {

    @NotBlank
    public UUID zaakUUID;

    @NotBlank
    public UUID roltypeUUID;

    @NotBlank
    public String roltoelichting;

    @NotBlank
    public IdentificatieType betrokkeneIdentificatieType;

    @NotBlank
    public String betrokkeneIdentificatie;
}
