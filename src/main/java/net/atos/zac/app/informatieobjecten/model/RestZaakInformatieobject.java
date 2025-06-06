/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.model;

import java.time.LocalDate;

import nl.info.zac.app.policy.model.RestZaakRechten;
import nl.info.zac.app.zaak.model.RestZaakStatus;

/**
 * weergave van een zaak die is gekoppeld aan een (enkelvoudig) informatieobject
 */
public class RestZaakInformatieobject {

    public String zaakIdentificatie;

    public RestZaakStatus zaakStatus;

    public LocalDate zaakStartDatum;

    public LocalDate zaakEinddatumGepland;

    public String zaaktypeOmschrijving;

    public RestZaakRechten zaakRechten;
}
