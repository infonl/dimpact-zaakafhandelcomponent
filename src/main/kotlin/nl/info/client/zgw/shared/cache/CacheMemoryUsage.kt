/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.cache

data class CacheMemoryUsage(
    val perCache: Map<String, Long>,
    val total: Long
)
