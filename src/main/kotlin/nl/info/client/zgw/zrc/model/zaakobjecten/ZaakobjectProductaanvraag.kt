/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI
import java.util.UUID

class ZaakobjectProductaanvraag : Zaakobject {
    companion object {
        const val OBJECT_TYPE_OVERIGE_PRODUCTAANVRAAG = "ProductAanvraag"
    }

    /**
     * No-arg constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with required attributes
     */
    constructor(zaak: URI, productaanvraag: URI?, url: URI, uuid: UUID) :
        super(zaak, productaanvraag, ObjectTypeEnum.OVERIGE, url, uuid) {
        objectTypeOverige = OBJECT_TYPE_OVERIGE_PRODUCTAANVRAAG
    }

    override val waarde: String?
        get() = `object`?.path?.substringAfterLast("/")
}
