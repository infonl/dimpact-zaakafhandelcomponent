/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.gebruikersvoorkeuren.model;

import java.util.List;

import net.atos.zac.app.policy.model.RestWerklijstRechten;

public class RESTTabelGegevens {

    public int aantalPerPagina;

    public List<Integer> pageSizeOptions;

    public RestWerklijstRechten werklijstRechten;


}
