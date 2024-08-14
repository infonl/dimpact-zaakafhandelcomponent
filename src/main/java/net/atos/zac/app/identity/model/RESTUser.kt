/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.identity.model;

import jakarta.validation.constraints.NotNull;

public class RESTUser {

    @NotNull
    public String id;

    public String naam;
}
