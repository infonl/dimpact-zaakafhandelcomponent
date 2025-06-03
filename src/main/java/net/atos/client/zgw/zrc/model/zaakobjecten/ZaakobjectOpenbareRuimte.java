/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model.zaakobjecten;

import java.net.URI;

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum;


/**
 * ZaakobjectOpenbareRuimte
 */
public class ZaakobjectOpenbareRuimte extends ZaakobjectMetObjectIdentificatie<ObjectOpenbareRuimte> {

    /**
     * Constructor for JSONB deserialization
     */
    public ZaakobjectOpenbareRuimte() {
    }

    /**
     * Constructor with required attributes
     */
    public ZaakobjectOpenbareRuimte(final URI zaak, final URI bagobjectURI, final ObjectOpenbareRuimte objectOpenbareRuimte) {
        super(zaak, bagobjectURI, ObjectTypeEnum.OPENBARE_RUIMTE, objectOpenbareRuimte);
    }

    @Override
    public String getWaarde() {
        return getObjectIdentificatie().getIdentificatie();
    }
}
