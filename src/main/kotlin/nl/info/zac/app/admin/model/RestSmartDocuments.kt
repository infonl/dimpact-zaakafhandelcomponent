/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestSmartDocuments(
    var enabledGlobally: Boolean,
    var enabledForZaaktype: Boolean
)
