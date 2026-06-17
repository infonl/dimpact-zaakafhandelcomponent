/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbNillable
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak

/**
 * Extension of [nl.info.client.zgw.zrc.model.generated.Zaak] to be able to delete the 'gerelateerde zaken' of a zaak
 * in ZGW JSON requests.
 */
open class NillableGerelateerdeZakenZaakPatch(
    /**
     * As per the ZGW ZRC API, to remove the gerelateerde zaken from a zaak, the gerelateerde zaken list needs to be set to `null`
     * in the ZGW API JSON request body.
     * Therefore, we override the parent `gerelateerdeZaken` property so that it can output a `null` JSON value.
     * Note that this results in 'This property hides Java field XXXX thus making it inaccessible.' compiler warning,
     **/
    @field:JsonbNillable
    private val gerelateerdeZaken: List<GerelateerdeZaak>?
) : Zaak() {
    override fun getGerelateerdeZaken() = gerelateerdeZaken
}
