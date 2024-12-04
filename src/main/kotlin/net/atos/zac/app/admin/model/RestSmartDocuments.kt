/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class RestSmartDocuments(
    var enabledGlobally: Boolean,
    var enabledForZaaktype: Boolean
)
