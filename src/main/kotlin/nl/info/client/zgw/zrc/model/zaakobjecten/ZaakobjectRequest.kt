/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

/**
 * Request body for `POST /zaakobjecten`. Unlike [Zaakobject], this has no `url`/`uuid`,
 * since those are server-assigned and never sent by the client.
 */
abstract class ZaakobjectRequest {
    /**
     * URL-referentie naar de ZAAK
     * - required
     */
    var zaak: URI

    /**
     * URL-referentie naar de resource die het OBJECT beschrijft
     */
    var `object`: URI? = null

    /**
     * Beschrijft het type OBJECT gerelateerd aan de ZAAK
     * - required
     */
    var objectType: ObjectTypeEnum

    /**
     * Beschrijft het type OBJECT als `objectType` de waarde "overige" heeft
     * - maxLength: 100
     * - pattern: '[a-z\_]+'
     */
    var objectTypeOverige: String? = null

    /**
     * Omschrijving van de betrekking tussen de ZAAK en het OBJECT
     * - maxLength: 80
     */
    var relatieomschrijving: String? = null

    /**
     * Constructor with required attributes
     */
    protected constructor(zaakUri: URI, objectUri: URI?, objectType: ObjectTypeEnum) {
        this.zaak = zaakUri
        this.`object` = objectUri
        this.objectType = objectType
    }
}
