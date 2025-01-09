/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.util

private const val BSN_LENGTH = 9
// https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
private const val BSN_11_PROEF = 11

fun String.validateBSN() {
    require(this.length == BSN_LENGTH) { "BSN '$this' length must be $BSN_LENGTH" }
    require(bsnSum(this) % BSN_11_PROEF == 0) { "Invalid BSN '$this'" }
}

private fun bsnSum(bsn: String) =
    bsn.mapIndexed { index, bsnChar ->
        require(bsnChar.isDigit()) { "Character on index $index in BSN '$bsn' is not a digit" }
        if (index == BSN_LENGTH - 1) {
            -bsnChar.digitToInt()
        } else {
            (BSN_LENGTH - index) * bsnChar.digitToInt()
        }
    }.sum()
