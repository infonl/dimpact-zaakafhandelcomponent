/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util

import java.net.URI
import java.util.UUID

fun uuidFromURI(uri: URI): UUID = extractUuid(uri.path).let(UUID::fromString)

fun extractedUuidIsEqual(a: URI?, b: URI?): Boolean =
    a?.let { b?.let { extractUuid(a.path) == extractUuid(b.path) } } ?: (a == null && b == null)

private fun extractUuid(path: String): String = path.substringAfterLast("/")