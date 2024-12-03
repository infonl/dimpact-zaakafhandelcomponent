/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util

import java.net.URI
import java.util.UUID

/**
 * Extracts the UUID from the last part of the path of the URI.
 */
fun URI.extractUuid(): UUID = extractUuid(this.path).let(UUID::fromString)

fun extractedUuidIsEqual(a: URI?, b: URI?): Boolean =
    a?.let { b?.let { extractUuid(a.path) == extractUuid(b.path) } } ?: (a == null && b == null)

private fun extractUuid(path: String): String = path.substringAfterLast("/")
