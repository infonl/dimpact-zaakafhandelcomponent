/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import nl.info.client.zgw.zrc.model.RolMedewerker
import nl.info.client.zgw.zrc.model.RolOrganisatorischeEenheid

fun createZaakToewijzing(
    groep: RolOrganisatorischeEenheid? = null,
    behandelaarRollen: List<RolMedewerker> = emptyList(),
    zaakspecifiekGeautoriseerdeMedewerkers: List<RolMedewerker> = emptyList(),
    isZaakspecifiekGeautoriseerd: Boolean = false
) = ZaakToewijzing(
    groep = groep,
    behandelaarRollen = behandelaarRollen,
    zaakspecifiekGeautoriseerdeMedewerkers = zaakspecifiekGeautoriseerdeMedewerkers,
    isZaakspecifiekGeautoriseerd = isZaakspecifiekGeautoriseerd
)
