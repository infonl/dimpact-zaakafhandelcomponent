/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.documentcreation.model;

import jakarta.json.bind.annotation.JsonbProperty;

public class Data {
    @JsonbProperty("startformulier")
    public StartformulierData startformulierData;

    @JsonbProperty("zaak")
    public ZaakData zaakData;

    @JsonbProperty("taak")
    public TaakData taakData;

    @JsonbProperty("gebruiker")
    public GebruikerData gebruikerData;

    @JsonbProperty("aanvrager")
    public AanvragerData aanvragerData;
}
