/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

public class ObjectOverige<T> {
    private T overigeData;

    /**
     * Constructor for JSONB deserialization
     */
    public ObjectOverige() {
    }

    /**
     * Constructor with required attributes
     */
    public ObjectOverige(final T overigeData) {
        this.overigeData = overigeData;
    }

    public T getOverigeData() {
        return overigeData;
    }

    public void setOverigeData(final T overigeData) {
        this.overigeData = overigeData;
    }
}
