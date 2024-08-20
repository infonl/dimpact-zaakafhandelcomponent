/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.kvk.vestigingsprofiel.model.generated.Adres
import net.atos.client.kvk.vestigingsprofiel.model.generated.SBIActiviteit
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
    restVestigingsprofiel.commercieleVestiging = isIndicatie(vestiging.indCommercieleVestiging)

    restVestigingsprofiel.type =
        if (isIndicatie(vestiging.indHoofdvestiging)) VESTIGINGTYPE_HOOFDVESTIGING else VESTIGINGTYPE_NEVENVESTIGING
    restVestigingsprofiel.sbiHoofdActiviteit = vestiging.sbiActiviteiten
        .stream()
        .filter { a: SBIActiviteit -> isIndicatie(a.indHoofdactiviteit) }
        .findAny()
        .map { obj: SBIActiviteit -> obj.sbiOmschrijving }
        .orElse(null)

    restVestigingsprofiel.sbiActiviteiten = vestiging.sbiActiviteiten
        .stream()
        .filter { a: SBIActiviteit -> !isIndicatie(a.indHoofdactiviteit) }
        .map { obj: SBIActiviteit -> obj.sbiOmschrijving }
        .toList()

    restVestigingsprofiel.adressen = vestiging.adressen
        .stream()
        .map { adres: Adres ->
            RestKlantenAdres(
                adres.type,
                isIndicatie(adres.indAfgeschermd),
                adres.volledigAdres
            )
        }
        .toList()

    restVestigingsprofiel.website = vestiging.websites?.first()
    return restVestigingsprofiel
}

private fun isIndicatie(stringIndicatie: String?): Boolean {
    if (stringIndicatie == null) {
        return false
    }
    return when (stringIndicatie.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> throw IllegalStateException("Unexpected value: $stringIndicatie")
    }
}
