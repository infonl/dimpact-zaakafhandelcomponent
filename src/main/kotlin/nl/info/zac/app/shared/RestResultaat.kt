/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

open class RestResultaat<T>(
    val resultaten: Collection<T> = emptyList(),
    aantalTotaal: Long = resultaten.size.toLong(),
    val foutmelding: String = ""
) {
    val totaal = aantalTotaal.toFloat()
}
