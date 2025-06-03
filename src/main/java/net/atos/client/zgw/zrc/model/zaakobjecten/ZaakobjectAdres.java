/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;


/**
 * ZaakobjectAdres
 */
public class ZaakobjectAdres extends ZaakobjectMetObjectIdentificatie<ObjectAdres> {

    /**
     * Constructor for JSONB deserialization
     */
    public ZaakobjectAdres() {
    }

    /**
     * Constructor with required attributes
     */
    public ZaakobjectAdres(final URI zaak, final URI bagobjectURI, final ObjectAdres adres) {
        super(zaak, bagobjectURI, ObjectTypeEnum.ADRES, adres);
    }

    @Override
    public String getWaarde() {
        return getObjectIdentificatie().getIdentificatie();
    }
}
