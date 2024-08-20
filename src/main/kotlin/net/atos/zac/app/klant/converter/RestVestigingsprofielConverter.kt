/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging
import net.atos.zac.app.klant.model.bedrijven.RestKlantenAdres
import net.atos.zac.app.klant.model.bedrijven.RestVestigingsprofiel
import java.util.Locale

private const val VESTIGINGTYPE_HOOFDVESTIGING = "HOOFDVESTIGING"
private const val VESTIGINGTYPE_NEVENVESTIGING = "NEVENVESTIGING"

fun convert(vestiging: Vestiging): RestVestigingsprofiel {
    val restVestigingsprofiel = RestVestigingsprofiel()
    restVestigingsprofiel.kvkNummer = vestiging.kvkNummer
    restVestigingsprofiel.vestigingsnummer = vestiging.vestigingsnummer
    restVestigingsprofiel.eersteHandelsnaam = vestiging.eersteHandelsnaam
    restVestigingsprofiel.rsin = vestiging.rsin
    restVestigingsprofiel.totaalWerkzamePersonen = vestiging.totaalWerkzamePersonen
    restVestigingsprofiel.deeltijdWerkzamePersonen = vestiging.deeltijdWerkzamePersonen
    restVestigingsprofiel.voltijdWerkzamePersonen = vestiging.voltijdWerkzamePersonen
    restVestigingsprofiel.commercieleVestiging = vestiging.indCommercieleVestiging?.isIndicatie() ?: false

    restVestigingsprofiel.type =
        if (vestiging.indHoofdvestiging?.isIndicatie() == true) VESTIGINGTYPE_HOOFDVESTIGING else VESTIGINGTYPE_NEVENVESTIGING
    restVestigingsprofiel.sbiHoofdActiviteit = vestiging.sbiActiviteiten
        .filter { it.indHoofdactiviteit?.isIndicatie() == true }
        .map { it.sbiOmschrijving }
        .firstOrNull()

    restVestigingsprofiel.sbiActiviteiten = vestiging.sbiActiviteiten
        .filter { it.indHoofdactiviteit?.isIndicatie() == false }
        .map { it.sbiOmschrijving }

    restVestigingsprofiel.adressen = vestiging.adressen
        .map {
            RestKlantenAdres(
                it.type,
                it.indAfgeschermd?.isIndicatie() ?: false,
                it.volledigAdres
            )
        }
        .toList()

    restVestigingsprofiel.website = vestiging.websites?.first()
    return restVestigingsprofiel
}

private fun String.isIndicatie(): Boolean =
    when (this.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> error("Unexpected value: $this")
    }
