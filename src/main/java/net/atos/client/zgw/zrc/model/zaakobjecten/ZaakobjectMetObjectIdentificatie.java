/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;

public abstract class ZaakobjectMetObjectIdentificatie<T> extends Zaakobject {

    /**
     * Het generieke object
     * - Required
     */
    private T objectIdentificatie;

    /**
     * Constructor for JSONB deserialization
     */
    protected ZaakobjectMetObjectIdentificatie() {
    }

    /**
     * Constructor with required attributes
     */
    protected ZaakobjectMetObjectIdentificatie(
            final URI zaak,
            final URI objectUri,
            final ObjectTypeEnum objectType,
            final T objectIdentificatie
    ) {
        super(zaak, objectUri, objectType);
        this.objectIdentificatie = objectIdentificatie;
    }

    public T getObjectIdentificatie() {
        return objectIdentificatie;
    }
}
