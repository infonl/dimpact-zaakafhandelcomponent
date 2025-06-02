/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;

/**
 * ZaakobjectNummeraanduiding
 */
public class ZaakobjectNummeraanduiding extends ZaakobjectMetObjectIdentificatie<ObjectOverige<ObjectNummeraanduiding>> {

    public static final String OBJECT_TYPE_OVERIGE = "nummeraanduiding";

    /**
     * Constructor for JSONB deserialization
     */
    public ZaakobjectNummeraanduiding() {
    }

    /**
     * Constructor with required attributes
     */
    public ZaakobjectNummeraanduiding(final URI zaak, final URI bagObjectUri, final ObjectNummeraanduiding nummeraanduiding) {
        super(zaak, bagObjectUri, ObjectTypeEnum.OVERIGE, new ObjectOverige<>(nummeraanduiding));
        setObjectTypeOverige(OBJECT_TYPE_OVERIGE);
    }

    @Override
    public String getWaarde() {
        return getObjectIdentificatie().overigeData.getIdentificatie();
    }
}
