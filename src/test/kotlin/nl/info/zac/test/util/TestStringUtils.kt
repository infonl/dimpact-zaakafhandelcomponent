/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.test.util

fun createRandomStringWithAlphanumericCharacters(stringLength: Int) = List(stringLength) {
    (('a'..'z') + ('A'..'Z') + ('0'..'9')).random()
}.joinToString("")
