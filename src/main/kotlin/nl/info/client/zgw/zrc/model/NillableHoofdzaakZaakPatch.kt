/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbNillable
import nl.info.client.zgw.zrc.model.generated.Zaak
import java.net.URI

/**
 * Extension of [nl.info.client.zgw.zrc.model.generated.Zaak] to be able to delete the hoofdzaak of a zaak
 * in ZGW JSON requests.
 */
open class NillableHoofdzaakZaakPatch(
    /**
     * As per the ZGW ZRC API, to remove the hoofdzaak from a zaak, the hoofdzaak needs to be set to `null`
     * in the ZGW API JSON request body.
     * Therefore, we override the parent `hoofdzaak` property that can it can output a `null` JSON value.
     * Note that this results in 'This property hides Java field XXXX thus making it inaccessible.' compiler warning,
     **/
    @field:JsonbNillable
    private val hoofdzaak: URI? = null
) : Zaak() {
    override fun getHoofdzaak(): URI? = hoofdzaak
}
