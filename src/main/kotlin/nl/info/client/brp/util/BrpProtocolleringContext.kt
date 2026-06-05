/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.enterprise.context.ApplicationScoped
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpProtocolleringContext {
    // ThreadLocal so coroutine IO threads (which have no CDI request scope) each get an isolated map.
    private val threadLocalHeaders = ThreadLocal.withInitial<MutableMap<String, String>> { mutableMapOf() }

    val headers: MutableMap<String, String>
        get() = threadLocalHeaders.get()

    fun clearHeaders() = threadLocalHeaders.get().clear()
}
