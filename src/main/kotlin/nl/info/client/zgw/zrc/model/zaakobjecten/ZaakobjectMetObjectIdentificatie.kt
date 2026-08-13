/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI
import java.util.UUID

abstract class ZaakobjectMetObjectIdentificatie<T> : Zaakobject {
    /**
     * Het generieke object
     * - Required
     */
    var objectIdentificatie: T? = null
        private set

    /**
     * Constructor for JSONB deserialization
     */
    protected constructor() : super()

    /**
     * Constructor with required attributes
     */
    protected constructor(
        zaak: URI,
        objectUri: URI?,
        objectType: ObjectTypeEnum,
        objectIdentificatie: T?,
        url: URI,
        uuid: UUID
    ) : super(zaak, objectUri, objectType, url, uuid) {
        this.objectIdentificatie = objectIdentificatie
    }
}
