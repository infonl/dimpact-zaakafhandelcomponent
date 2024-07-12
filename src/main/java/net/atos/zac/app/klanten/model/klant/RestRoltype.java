/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.klanten.model.klant;

import java.util.UUID;

import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum;

public class RestRoltype {
    public UUID uuid;

    public String naam;

    public OmschrijvingGeneriekEnum omschrijvingGeneriekEnum;
}
