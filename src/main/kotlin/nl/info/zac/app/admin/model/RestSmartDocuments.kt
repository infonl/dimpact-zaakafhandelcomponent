/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestSmartDocuments(
    var enabledGlobally: Boolean,
    var enabledForZaaktype: Boolean
)

fun ZaaktypeConfiguration.toRestSmartDocuments(enabledGlobally: Boolean) = RestSmartDocuments(
    enabledGlobally = enabledGlobally,
    enabledForZaaktype = smartDocumentsIngeschakeld
)
