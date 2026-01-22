/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.sensitive

import jakarta.enterprise.context.ApplicationScoped
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
@AllOpen
@NoArgConstructor
class SensitiveDataService {
    val storage = ConcurrentHashMap<UUID, String>()

    fun put(data: String): UUID =
        UUID.randomUUID().also {
            storage[it] = data
        }

    fun get(key: UUID): String? = storage[key]
}
