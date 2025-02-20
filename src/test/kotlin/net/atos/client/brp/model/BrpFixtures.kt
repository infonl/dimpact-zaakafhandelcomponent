/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp.model

import net.atos.client.brp.model.generated.AbstractVerblijfplaats
import net.atos.client.brp.model.generated.Adres
import net.atos.client.brp.model.generated.Adressering
import net.atos.client.brp.model.generated.AdresseringBeperkt
import net.atos.client.brp.model.generated.OpschortingBijhouding
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.PersoonBeperkt
import net.atos.client.brp.model.generated.PersoonInOnderzoek
import net.atos.client.brp.model.generated.PersoonInOnderzoekBeperkt
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.RniDeelnemer
import net.atos.client.brp.model.generated.VerblijfadresBinnenland
import net.atos.client.brp.model.generated.Waardetabel

fun createAdres(
    verblijfAdresBinnenland: VerblijfadresBinnenland = createVerblijfadresBinnenland()
) = Adres().apply {
    verblijfadres = verblijfAdresBinnenland
}

fun createAdressering(
    adresRegel1: String = "adresRegel1",
    adresRegel2: String = "adresRegel2",
    adresRegel3: String = "adresRegel3",
    land: Waardetabel = createWaardeTabel()
) = Adressering().apply {
    adresregel1 = adresRegel1
    adresregel2 = adresRegel2
    adresregel3 = adresRegel3
    this.land = land
}

@Suppress("LongParameterList")
fun createPersoon(
    bsn: String = "123456789",
    age: Int = 18,
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoek? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    indicationCuratoriesRegister: Boolean? = false,
    rniDeelnemerList: List<RniDeelnemer>? = null,
    address: Adressering? = null,
    verblijfplaats: AbstractVerblijfplaats? = null
) =
    Persoon().apply {
        burgerservicenummer = bsn
        leeftijd = age
        geheimhoudingPersoonsgegevens = confidentialPersonalData
        inOnderzoek = personInResearch
        opschortingBijhouding = suspensionMaintenance
        indicatieCurateleRegister = indicationCuratoriesRegister
        rni = rniDeelnemerList
        adressering = address
        this.verblijfplaats = verblijfplaats
    }

@Suppress("LongParameterList")
fun createPersoonBeperkt(
    bsn: String = "123456789",
    age: Int = 18,
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoekBeperkt? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    rniDeelnemerList: List<RniDeelnemer>? = null,
    address: AdresseringBeperkt? = null
) = PersoonBeperkt().apply {
    burgerservicenummer = bsn
    leeftijd = age
    burgerservicenummer = "burgerservicenummer"
    geheimhoudingPersoonsgegevens = confidentialPersonalData
    inOnderzoek = personInResearch
    opschortingBijhouding = suspensionMaintenance
    rni = rniDeelnemerList
    adressering = address
}

fun createRaadpleegMetBurgerservicenummer(
    burgerservicenummers: List<String> = listOf("123456789")
) = RaadpleegMetBurgerservicenummer().apply {
    this.burgerservicenummer = burgerservicenummers
}

fun createRaadpleegMetBurgerservicenummerResponse(
    persons: List<Persoon> = listOf(createPersoon())
) = RaadpleegMetBurgerservicenummerResponse().apply {
    personen = persons
}

fun createVerblijfadresBinnenland(
    officieleStraatnaam: String = "officieleStraatnaam",
    huisnummer: Int = 123,
    postcode: String = "postcode",
    woonplaats: String = "woonplaats"
) = VerblijfadresBinnenland().apply {
    this.officieleStraatnaam = officieleStraatnaam
    this.huisnummer = huisnummer
    this.postcode = postcode
    this.woonplaats = woonplaats
}

fun createWaardeTabel(
    code: String = "dummyCode",
    omschrijving: String = "dummyOmschrijving"
) = Waardetabel().apply {
    this.code = code
    this.omschrijving = omschrijving
}
