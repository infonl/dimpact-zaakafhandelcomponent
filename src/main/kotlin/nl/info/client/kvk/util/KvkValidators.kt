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

fun String.validateKvKVestigingsnummer() {
    require(this.isValidKvkVestigingsnummer()) {
        "KvK vestigingsnummer must be $KVK_VESTIGINGSNUMMER_LENGTH digits long and contain only digits"
    }
}

fun String.validateKvkNummer() {
    require(this.isValidKvkNummer()) { "KvK nummer must be $KVK_NUMMER_LENGTH digits long and contain only digits" }
}
