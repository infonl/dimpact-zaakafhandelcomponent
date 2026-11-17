/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

fun formatHuisnummer(huisnummer: Int?, huisletter: String?, huisnummertoevoeging: String?) =
    listOf(listOf(huisnummer?.toString(), huisletter).joinNonBlank(), huisnummertoevoeging).joinNonBlank(" ")
