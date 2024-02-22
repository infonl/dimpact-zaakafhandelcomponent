/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.notities.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public class RESTNotitie {

    public Long id;

    public UUID zaakUUID;

    public String tekst;

    public ZonedDateTime tijdstipLaatsteWijziging;

    public String gebruikersnaamMedewerker;

    public String voornaamAchternaamMedewerker;

    public boolean bewerkenToegestaan;
}
