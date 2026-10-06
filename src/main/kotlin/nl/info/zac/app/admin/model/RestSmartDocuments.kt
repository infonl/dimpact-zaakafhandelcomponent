/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestSmartDocuments(
    @get:JsonbProperty("isEnabledGlobally")
    @set:JsonbProperty("isEnabledGlobally")
    var isEnabledGlobally: Boolean,

    @get:JsonbProperty("isEnabledForZaaktype")
    @set:JsonbProperty("isEnabledForZaaktype")
    var isEnabledForZaaktype: Boolean
)

fun ZaaktypeConfiguration.toRestSmartDocuments(enabledGlobally: Boolean) = RestSmartDocuments(
    isEnabledGlobally = enabledGlobally,
    isEnabledForZaaktype = isSmartDocumentsEnabled
)
