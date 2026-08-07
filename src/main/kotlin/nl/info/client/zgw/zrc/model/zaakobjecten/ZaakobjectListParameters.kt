/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import jakarta.ws.rs.QueryParam
import nl.info.client.zgw.zrc.model.generated.ObjectTypeEnum
import java.net.URI

class ZaakobjectListParameters {
    /**
     * URL-referentie naar de ZAAK
     */
    @QueryParam("zaak")
    var zaak: URI? = null

    /**
     * URL-referentie naar de resource die het OBJECT beschrijft
     */
    @QueryParam("object")
    var `object`: URI? = null

    private var objectTypeValue: String? = null

    /**
     * Beschrijft het type OBJECT gerelateerd aan de ZAAK. Als er geen passend type is, dan moet het type worden opgegeven onder
     * `objectTypeOverige`
     */
    var objectType: ObjectTypeEnum?
        @QueryParam("objectType")
        get() = objectTypeValue?.let(ObjectTypeEnum::fromValue)
        set(value) {
            objectTypeValue = value.toString()
        }

    /**
     * Een pagina binnen de gepagineerde set resultaten
     */
    @QueryParam("page")
    var page: Int? = null

    override fun toString(): String =
        "ZaakobjectListParameters{" +
            "zaak=$zaak" +
            ", object=${`object`}" +
            ", objectType='$objectTypeValue'" +
            ", page=$page" +
            '}'
}
