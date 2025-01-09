/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Locale

private const val VESTIGINGTYPE_HOOFDVESTIGING = "HOOFDVESTIGING"
private const val VESTIGINGTYPE_NEVENVESTIGING = "NEVENVESTIGING"

@AllOpen
@NoArgConstructor
data class RestVestigingsprofiel(
    var adressen: List<RestKlantenAdres>? = null,
    var commercieleVestiging: Boolean = false,
    var deeltijdWerkzamePersonen: Int = 0,
    var eersteHandelsnaam: String? = null,
    var kvkNummer: String? = null,
    var rsin: String? = null,
    var sbiActiviteiten: List<String>? = null,
    var sbiHoofdActiviteit: String? = null,
    var type: String? = null,
    var totaalWerkzamePersonen: Int = 0,
    var vestigingsnummer: String? = null,
    var voltijdWerkzamePersonen: Int = 0,
    var website: String? = null
)

fun Vestiging.toRestVestigingsProfiel() = RestVestigingsprofiel(
    kvkNummer = this.kvkNummer,
    vestigingsnummer = this.vestigingsnummer,
    eersteHandelsnaam = this.eersteHandelsnaam,
    rsin = this.rsin,
    totaalWerkzamePersonen = this.totaalWerkzamePersonen,
    deeltijdWerkzamePersonen = this.deeltijdWerkzamePersonen,
    voltijdWerkzamePersonen = this.voltijdWerkzamePersonen,
    commercieleVestiging = this.indCommercieleVestiging?.isIndicatie() ?: false,
    type = if (this.indHoofdvestiging?.isIndicatie() == true) VESTIGINGTYPE_HOOFDVESTIGING else VESTIGINGTYPE_NEVENVESTIGING,
    sbiHoofdActiviteit = this.sbiActiviteiten
        .filter { it.indHoofdactiviteit?.isIndicatie() == true }
        .map { it.sbiOmschrijving }
        .firstOrNull(),
    sbiActiviteiten = this.sbiActiviteiten
        .filter { it.indHoofdactiviteit?.isIndicatie() == false }
        .map { it.sbiOmschrijving },
    adressen = this.adressen
        .map {
            RestKlantenAdres(
                it.type,
                it.indAfgeschermd?.isIndicatie() ?: false,
                it.volledigAdres
            )
        }
        .toList(),
    website = this.websites?.first()
)

private fun String.isIndicatie(): Boolean =
    when (this.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> error("Unexpected value: $this")
    }
