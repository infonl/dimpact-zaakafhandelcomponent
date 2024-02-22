/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klanten.model.klant;

import java.util.UUID;

import net.atos.client.zgw.ztc.model.generated.RolType;

public class RESTRoltype {
    public UUID uuid;

    public String naam;

    public RolType.OmschrijvingGeneriekEnum omschrijvingGeneriekEnum;
}
