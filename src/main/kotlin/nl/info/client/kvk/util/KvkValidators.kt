/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import java.lang.Character.isDigit

const val KVK_VESTIGINGSNUMMER_LENGTH = 12
const val KVK_NUMMER_LENGTH = 8

fun String.isValidKvkVestigingsnummer() = this.length == KVK_VESTIGINGSNUMMER_LENGTH && this.all(::isDigit)
fun String.isValidKvkNummer() = this.length == KVK_NUMMER_LENGTH && this.all(::isDigit)
