/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

/**
 * ZaakobjectAdres
 */
class ZaakobjectAdres : ZaakobjectMetObjectIdentificatie<ObjectAdres> {
    /**
     * Constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with required attributes
     */
    constructor(zaak: URI?, bagobjectURI: URI?, adres: ObjectAdres?) : super(zaak, bagobjectURI, ObjectTypeEnum.ADRES, adres)

    override val waarde: String?
        get() = objectIdentificatie?.identificatie
}
