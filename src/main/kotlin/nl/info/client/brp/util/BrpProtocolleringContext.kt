/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.enterprise.context.RequestScoped
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@RequestScoped
@AllOpen
@NoArgConstructor
class BrpProtocolleringContext {
    val headers: MutableMap<String, String> = mutableMapOf()
}
