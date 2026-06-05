/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import nl.info.client.kvk.basisprofiel.model.generated.Basisprofiel
import nl.info.client.kvk.vestigingsprofiel.model.generated.Vestiging
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

private const val VESTIGINGTYPE_HOOFDVESTIGING = "HOOFDVESTIGING"
private const val VESTIGINGTYPE_NEVENVESTIGING = "NEVENVESTIGING"

@AllOpen
@NoArgConstructor
data class RestBedrijfsprofiel(
    var adressen: List<RestBedrijfAdres>? = null,
    var commercieleVestiging: Boolean = false,
    var deeltijdWerkzamePersonen: Int? = null,
    var eersteHandelsnaam: String? = null,
    var kvkNummer: String? = null,
    var rsin: String? = null,
    var sbiActiviteiten: List<String>? = null,
    var sbiHoofdActiviteit: String? = null,
    var type: String? = null,
    var totaalWerkzamePersonen: Int? = null,
    var vestigingsnummer: String? = null,
    var voltijdWerkzamePersonen: Int? = null,
    var website: String? = null,
    var rechtsvorm: String? = null,
    var uitgebreideRechtsvorm: String? = null,
    var statutaireNaam: String? = null
)

fun Vestiging.toRestBedrijfsprofiel() = RestBedrijfsprofiel(
    kvkNummer = this.kvkNummer,
    vestigingsnummer = this.vestigingsnummer,
    eersteHandelsnaam = this.eersteHandelsnaam,
    rsin = this.rsin,
    totaalWerkzamePersonen = this.totaalWerkzamePersonen,
    deeltijdWerkzamePersonen = this.deeltijdWerkzamePersonen,
    voltijdWerkzamePersonen = this.voltijdWerkzamePersonen,
    commercieleVestiging = this.indCommercieleVestiging?.isIndicatie() ?: false,
    type = if (this.indHoofdvestiging?.isIndicatie() == true) VESTIGINGTYPE_HOOFDVESTIGING else VESTIGINGTYPE_NEVENVESTIGING,
    sbiHoofdActiviteit = this.sbiActiviteiten?.firstOrNull {
        it?.indHoofdactiviteit?.isIndicatie() == true
    }?.sbiOmschrijving,
    sbiActiviteiten = this.sbiActiviteiten?.filter {
        it.indHoofdactiviteit?.isIndicatie() == false
    }?.map { it.sbiOmschrijving },
    adressen = this.adressen?.map { it.toRestBedrijfAdres() },
    website = this.websites?.first()
)

fun Basisprofiel.toRestBedrijfsprofiel() = RestBedrijfsprofiel(
    kvkNummer = this.kvkNummer,
    eersteHandelsnaam = this.handelsnamen?.minByOrNull { it.volgorde ?: Int.MAX_VALUE }?.naam,
    totaalWerkzamePersonen = this.totaalWerkzamePersonen,
    sbiHoofdActiviteit = this.sbiActiviteiten?.firstOrNull {
        it?.indHoofdactiviteit?.isIndicatie() == true
    }?.sbiOmschrijving,
    sbiActiviteiten = this.sbiActiviteiten?.filter {
        it.indHoofdactiviteit?.isIndicatie() == false
    }?.map { it.sbiOmschrijving },
    rsin = this.embedded?.eigenaar?.rsin,
    rechtsvorm = this.embedded?.eigenaar?.rechtsvorm,
    uitgebreideRechtsvorm = this.embedded?.eigenaar?.uitgebreideRechtsvorm,
    adressen = this.embedded?.eigenaar?.adressen?.map { it.toRestBedrijfAdres() },
    website = this.embedded?.eigenaar?.websites?.firstOrNull(),
    statutaireNaam = this.statutaireNaam
)
