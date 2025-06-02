/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;

/**
 * ZaakobjectPand
 */
public class ZaakobjectPand extends ZaakobjectMetObjectIdentificatie<ObjectPand> {

    /**
     * Constructor for JSONB deserialization
     */
    public ZaakobjectPand() {
    }

    /**
     * Constructor with all required fields.
     */
    public ZaakobjectPand(final URI zaak, final URI bagobjectUri, final ObjectPand pand) {
        super(zaak, bagobjectUri, ObjectTypeEnum.PAND, pand);
    }

    @Override
    public String getWaarde() {
        return getObjectIdentificatie().getIdentificatie();
    }
}
