/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.model.audit;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;

import net.atos.client.zgw.shared.model.ObjectType;
import net.atos.client.zgw.shared.util.AuditWijzigingJsonbDeserializer;

@JsonbTypeDeserializer(AuditWijzigingJsonbDeserializer.class)
public abstract class AuditWijziging<T> {
    private T oud;

    private T nieuw;

    public T getOud() {
        return oud;
    }

    public void setOud(final T oud) {
        this.oud = oud;
    }

    public T getNieuw() {
        return nieuw;
    }

    public void setNieuw(final T nieuw) {
        this.nieuw = nieuw;
    }

    public abstract ObjectType getObjectType();

}
