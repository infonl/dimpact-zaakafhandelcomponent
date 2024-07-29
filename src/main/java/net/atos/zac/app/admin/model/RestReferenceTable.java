/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin.model;

import java.util.List;

public class RestReferenceTable {

    public Long id;

    public String code;

    public String naam;

    public boolean systeem;

    public int aantalWaarden;

    public List<RestReferenceTableValue> waarden;

    public RestReferenceTable() {
    }
}
