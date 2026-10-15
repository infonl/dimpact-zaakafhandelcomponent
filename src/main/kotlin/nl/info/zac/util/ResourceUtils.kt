/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import java.io.InputStream

fun ClassLoader.readResourceBytes(path: String): ByteArray = openResource(path).use { it.readAllBytes() }

fun ClassLoader.readNonBlankResourceLines(path: String): List<String> =
    openResource(path).bufferedReader().useLines { lines -> lines.filter { it.isNotBlank() }.toList() }

private fun ClassLoader.openResource(path: String): InputStream =
    getResourceAsStream(path) ?: error("Resource '$path' not found")
