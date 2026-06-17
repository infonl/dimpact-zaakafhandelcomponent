/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model

import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak

/**
 * Extension of [nl.info.client.zgw.zrc.model.generated.Zaak] to be able to update the 'gerelateerde zaken' of a zaak
 * in ZGW JSON requests.
 * Note that this results in 'This property hides Java field XXXX thus making it inaccessible.' compiler warning.
 */
open class GerelateerdeZakenZaakPatch(
    private val gerelateerdeZaken: List<GerelateerdeZaak>
) : Zaak() {
    override fun getGerelateerdeZaken() = gerelateerdeZaken
}
