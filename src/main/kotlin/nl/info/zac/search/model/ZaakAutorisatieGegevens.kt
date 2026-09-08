/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

/**
 * The zaak-level facts a zoekobject conversion needs but cannot derive from the object it converts.
 *
 * [geautoriseerdeMedewerkers] is resolved lazily because converting a zaak already knows them, while
 * converting a taak or a document would otherwise need an extra call to the zaakregister for every single
 * one of them.
 */
class ZaakAutorisatieGegevens(
    val isZaakspecifiekGeautoriseerd: Boolean,
    geautoriseerdeMedewerkersSupplier: () -> List<String>
) {
    val geautoriseerdeMedewerkers: List<String> by lazy(geautoriseerdeMedewerkersSupplier)
}
