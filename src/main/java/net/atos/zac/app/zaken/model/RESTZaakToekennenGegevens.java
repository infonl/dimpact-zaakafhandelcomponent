/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model;

import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RESTZaakToekennenGegevens {

    @NotNull public UUID zaakUUID;

    /**
     * Since this is used for the 'identificatie' field in
     * {@link net.atos.client.zgw.zrc.model.OrganisatorischeEenheid}
     * we need to make sure it adheres to the same constraints.
     */
    @Nullable @Size(max = 24)
    public String groepId;

    public String behandelaarGebruikersnaam;

    public String reden;
}
