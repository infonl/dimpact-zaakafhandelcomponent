/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.test.util

fun createRandomStringWithAlphanumericCharacters(stringLength: Int) = List(stringLength) {
    (('a'..'z') + ('A'..'Z') + ('0'..'9')).random()
}.joinToString("")
