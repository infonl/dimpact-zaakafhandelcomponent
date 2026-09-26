/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.RolOrganisatorischeEenheid

/**
 * Who a zaak is assigned to and who is individually authorised for it.
 *
 * Read once per zaak from a single set of rollen so that the authorisation policy, the search index and
 * the assignment logic all decide on the same data.
 */
data class ZaakToewijzing(
    val groep: RolOrganisatorischeEenheid?,
    /**
     * Normally at most one. A zaak can end up with several when rollen were created outside ZAC.
     */
    val behandelaarRollen: List<RolMedewerker>,
    val zaakspecifiekGeautoriseerdeMedewerkers: List<RolMedewerker>,
    val isZaakspecifiekGeautoriseerd: Boolean
) {
    val groepId: String?
        get() = groep?.identificatienummer

    val behandelaar: RolMedewerker?
        get() = behandelaarRollen.singleOrNull()

    val behandelaarId: String?
        get() = behandelaar?.identificatienummer

    /**
     * The medewerkers that are individually authorised for this zaak when it is zaakspecifiek geautoriseerd.
     */
    val geautoriseerdeMedewerkerIds: Set<String>
        get() = (behandelaarRollen + zaakspecifiekGeautoriseerdeMedewerkers)
            .mapNotNull { it.identificatienummer }
            .toSet()

    fun isGeautoriseerdeMedewerker(userId: String) = userId in geautoriseerdeMedewerkerIds
}
