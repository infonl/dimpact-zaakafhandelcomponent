/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

class ZaakobjectNummeraanduiding : ZaakobjectMetObjectIdentificatie<ObjectOverige<ObjectNummeraanduiding>> {
    companion object {
        const val OBJECT_TYPE_OVERIGE_NUMMERAANDUIDING = "nummeraanduiding"
    }

    /**
     * Constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with required attributes
     */
    constructor(zaak: URI, bagObjectUri: URI?, nummeraanduiding: ObjectNummeraanduiding?) : super(
        zaak,
        bagObjectUri,
        ObjectTypeEnum.OVERIGE,
        ObjectOverige(nummeraanduiding)
    ) {
        objectTypeOverige = OBJECT_TYPE_OVERIGE_NUMMERAANDUIDING
    }

    override val waarde: String?
        get() = objectIdentificatie?.overigeData?.identificatie
}
