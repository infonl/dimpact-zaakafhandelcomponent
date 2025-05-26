/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres
import java.util.Locale

data class RestKlantenAdres(
    /**
     * Correspondentieadres en/of bezoekadres
     */
    var type: String,
    var afgeschermd: Boolean,
    var volledigAdres: String
)

fun Adres.toRestKlantenAdres() = RestKlantenAdres(
    this.type,
    this.indAfgeschermd?.isIndicatie() == true,
    this.volledigAdres
)

fun String.isIndicatie(): Boolean =
    when (this.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> error("Unexpected value: $this")
    }
