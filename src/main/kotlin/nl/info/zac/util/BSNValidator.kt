/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

private const val BSN_LENGTH = 9
private const val BSN_11_PROEF = 11 // https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef

fun String.validateBSN(numberDescription: String) {
    require(this.length == BSN_LENGTH) { "$numberDescription '$this' length must be $BSN_LENGTH" }
    require(isElfProef(bsnSum(numberDescription, this))) { "Invalid $numberDescription '$this'" }
}

fun String.validateRSIN(numberDescription: String) = validateBSN(numberDescription)

private fun bsnSum(numberDescription: String, bsn: String) =
    bsn.mapIndexed { index, bsnChar ->
        require(bsnChar.isDigit()) { "Character on index $index in $numberDescription '$bsn' is not a digit" }
        if (index == BSN_LENGTH - 1) {
            -bsnChar.digitToInt()
        } else {
            (BSN_LENGTH - index) * bsnChar.digitToInt()
        }
    }.sum()

// https://nl.wikipedia.org/wiki/Burgerservicenummer#11-proef
private fun isElfProef(sum: Int) = sum % BSN_11_PROEF == 0
