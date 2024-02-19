/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.input;

import jakarta.json.bind.annotation.JsonbProperty;

public class TaakData {

    public boolean open;

    @JsonbProperty("zaak_open")
    public boolean zaakOpen;

    public String zaaktype;

}
