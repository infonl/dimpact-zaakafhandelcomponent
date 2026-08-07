/*
 * SPDX-FileCopyrightText: 2023, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

/**
 * ZaakobjectOpenbareRuimte
 */
class ZaakobjectOpenbareRuimte : ZaakobjectMetObjectIdentificatie<ObjectOpenbareRuimte> {
    /**
     * Constructor for JSONB deserialization
     */
    constructor() : super()

    /**
     * Constructor with required attributes
     */
    constructor(
        zaak: URI?,
        bagobjectURI: URI?,
        objectOpenbareRuimte: ObjectOpenbareRuimte?
    ) : super(zaak, bagobjectURI, ObjectTypeEnum.OPENBARE_RUIMTE, objectOpenbareRuimte)

    override val waarde: String?
        get() = objectIdentificatie?.identificatie
}
