/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import nl.info.client.zgw.ztc.model.generated.ResultaatType

/**
 * A resultaattype of a zaaktype.
 *
 * @property datumKenmerkOmschrijving the definitie of the eigenschap of the zaaktype that the resultaattype derives
 * its brondatum archiefprocedure from, or null when the resultaattype does not derive it from an eigenschap
 */
data class ZaaktypeResultaattype(
    val resultaatType: ResultaatType,
    val datumKenmerkOmschrijving: String? = null
)
