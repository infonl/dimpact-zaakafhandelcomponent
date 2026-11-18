/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * A zaaktype that a beheerder can filter the Zaakafhandelparameters page on.
 */
@NoArgConstructor
@AllOpen
data class RestZaaktypeFilterOptie(
    var identificatie: String,
    var omschrijving: String
)
