/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

class ZaakobjectPand : ZaakobjectMetObjectIdentificatie<ObjectPand> {
    /**
     * Constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with all required fields.
     */
    constructor(zaak: URI, bagobjectUri: URI?, pand: ObjectPand?) : super(zaak, bagobjectUri, ObjectTypeEnum.PAND, pand)

    override val waarde: String?
        get() = objectIdentificatie?.identificatie
}
