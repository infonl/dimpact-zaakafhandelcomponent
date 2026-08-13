/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI
import java.util.UUID

class ZaakobjectPand : ZaakobjectMetObjectIdentificatie<ObjectPand> {
    /**
     * Constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with all required fields.
     */
    constructor(zaak: URI, bagobjectUri: URI?, pand: ObjectPand?, url: URI, uuid: UUID) :
        super(zaak, bagobjectUri, ObjectTypeEnum.PAND, pand, url, uuid)

    override val waarde: String?
        get() = objectIdentificatie?.identificatie
}
