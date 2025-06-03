/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;


/**
 * ZaakobjectWoonplaats
 */
public class ZaakobjectWoonplaats extends ZaakobjectMetObjectIdentificatie<ObjectWoonplaats> {

    /**
     * Constructor for JSONB deserialization
     */
    public ZaakobjectWoonplaats() {
    }

    /**
     * Constructor with required attributes
     */
    public ZaakobjectWoonplaats(final URI zaak, final URI bagobjectUri, final ObjectWoonplaats woonplaats) {
        super(zaak, bagobjectUri, ObjectTypeEnum.WOONPLAATS, woonplaats);
    }

    @Override
    public String getWaarde() {
        return getObjectIdentificatie().getIdentificatie();
    }

}
