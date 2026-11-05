/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.logfilter

const val BSN_VISIBLE_DIGITS = 3
const val MASKED_VALUE = "*"

private val BSN_REGEX = Regex("(?<![0-9A-Za-z])[0-9]{9}(?![0-9A-Za-z])")
private val IBAN_REGEX = Regex(
    "(?<![0-9A-Za-z])[A-Z]{2}[0-9]{2}( ?[A-Z0-9]{4}){2} ?[A-Z0-9]{3,4}( (?![A-Z]{2}[0-9]{2})[A-Z0-9]{4}|[A-Z0-9]{4}){0,5}" +
        "( ?[A-Z0-9]{1,3})?(?![0-9A-Za-z])"
)

fun String.maskPersonalData() =
    replace(BSN_REGEX) { MASKED_VALUE.repeat(it.value.length - BSN_VISIBLE_DIGITS) + it.value.takeLast(BSN_VISIBLE_DIGITS) }
        .replace(IBAN_REGEX) { MASKED_VALUE.repeat(it.value.length) }
