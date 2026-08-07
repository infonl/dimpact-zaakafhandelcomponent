/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

/**
 * ZaakobjectWoonplaats
 */
class ZaakobjectWoonplaats : ZaakobjectMetObjectIdentificatie<ObjectWoonplaats> {
    /**
     * Constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with required attributes
     */
    constructor(
        zaak: URI?,
        bagobjectUri: URI?,
        woonplaats: ObjectWoonplaats?
    ) : super(zaak, bagobjectUri, ObjectTypeEnum.WOONPLAATS, woonplaats)

    override val waarde: String?
        get() = objectIdentificatie?.identificatie
}
