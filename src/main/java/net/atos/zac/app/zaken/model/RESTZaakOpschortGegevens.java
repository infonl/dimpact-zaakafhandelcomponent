/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model;

import java.time.LocalDate;

public class RESTZaakOpschortGegevens {

    public boolean indicatieOpschorting;

    public String redenOpschorting;

    public long duurDagen;

    public LocalDate einddatumGepland;

    public LocalDate uiterlijkeEinddatumAfdoening;
}
