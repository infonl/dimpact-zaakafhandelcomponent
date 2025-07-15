/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbNillable
import nl.info.client.zgw.zrc.model.generated.RelevanteZaak
import nl.info.client.zgw.zrc.model.generated.Zaak

/**
 * Extension of [nl.info.client.zgw.zrc.model.generated.Zaak] to be able to delete the 'relevante zaken' of a zaak
 * in ZGW JSON requests.
 */
open class NillableRelevanteZakenZaakPatch(
    /**
     * As per the ZGW ZRC API, to remove the relevante zaken from a zaak, the relevante zaken list needs to be set to `null`
     * in the ZGW API JSON request body.
     * Therefore, we override the parent `relevanteAndereZaken` property so that it can output a `null` JSON value.
     * Note that this results in 'This property hides Java field XXXX thus making it inaccessible.' compiler warning,
     **/
    @field:JsonbNillable
    private val relevanteAndereZaken: List<RelevanteZaak>?
) : Zaak() {
    override fun getRelevanteAndereZaken() = relevanteAndereZaken
}
