/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

const val KVK_VESTIGINGSNUMMER_LENGTH = 12

fun String.isValidKvkVestigingsnummer() = this.length == KVK_VESTIGINGSNUMMER_LENGTH && this.all { it.isDigit() }
