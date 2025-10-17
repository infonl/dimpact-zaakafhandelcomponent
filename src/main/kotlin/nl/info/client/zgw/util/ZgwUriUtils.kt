/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import java.net.URI
import java.util.UUID

/**
 * Extracts the UUID from the last part of the path of the ZGW resource URI (i.e.: after the last '/').
 *
 * @throws IllegalArgumentException if the last part of the path cannot be converted to a UUID
 */
fun URI.extractUuid(): UUID = extractUuid(this.path).let(UUID::fromString)

fun extractedUuidIsEqual(a: URI?, b: URI?): Boolean =
    a?.let { b?.let { extractUuid(a.path) == extractUuid(b.path) } } ?: (a == null && b == null)

/**
 * For security reasons, validate that the provided ZGW API URI starts with the value of the configured
 * ZGW API clients base URI.
 */
fun validateZgwApiUri(zgwApiURI: URI, zgwApiBaseURI: String) {
    check(zgwApiURI.toString().startsWith(zgwApiBaseURI)) {
        "URI '$zgwApiURI' does not start with value for configured ZGW API clients base URI: '$zgwApiBaseURI'"
    }
}

private fun extractUuid(path: String): String = path.substringAfterLast("/")
