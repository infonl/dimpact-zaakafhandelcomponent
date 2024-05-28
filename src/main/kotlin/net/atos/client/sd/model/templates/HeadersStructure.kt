/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class HeadersStructure(
    // TODO: clarify what do we have in headers
    var headerGroups: List<Any>,
    var accessible: Boolean = false
)
