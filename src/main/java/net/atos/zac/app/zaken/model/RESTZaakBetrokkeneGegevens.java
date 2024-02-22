/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import net.atos.zac.app.klanten.model.klant.IdentificatieType;

public class RESTZaakBetrokkeneGegevens {

    @NotNull public UUID zaakUUID;

    @NotNull public UUID roltypeUUID;

    @NotBlank public String roltoelichting;

    @NotNull public IdentificatieType betrokkeneIdentificatieType;

    @NotBlank public String betrokkeneIdentificatie;
}
