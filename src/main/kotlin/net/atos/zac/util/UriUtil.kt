/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util

import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.util.UUID

fun uuidFromURI(uri: URI): UUID = uuidFromURI(uri.getPath())

fun uuidFromURI(uri: String): UUID = UUID.fromString(extractUUID(uri))

fun isEqual(a: URI?, b: URI?): Boolean =
    if (a != null && b != null) (extractUUID(a.getPath()) == extractUUID(b.getPath())) else a == null && b == null

private fun extractUUID(path: String): String =
    if (StringUtils.contains(path, "/")) StringUtils.substringAfterLast(path, "/") else path
