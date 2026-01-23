/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

public class ObjectOverige<OVERIGE> {

    public OVERIGE overigeData;

    /**
     * Constructor for JSONB deserialization
     */
    public ObjectOverige() {
    }

    /**
     * Constructor with required attributes
     */
    public ObjectOverige(final OVERIGE overigeData) {
        this.overigeData = overigeData;
    }

    public OVERIGE getOverigeData() {
        return overigeData;
    }

    public void setOverigeData(final OVERIGE overigeData) {
        this.overigeData = overigeData;
    }
}
