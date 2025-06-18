/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp.model

import nl.info.client.brp.model.generated.AbstractVerblijfplaats
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Adressering
import nl.info.client.brp.model.generated.AdresseringBeperkt
import nl.info.client.brp.model.generated.OpschortingBijhouding
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.model.generated.PersoonBeperkt
import nl.info.client.brp.model.generated.PersoonInOnderzoek
import nl.info.client.brp.model.generated.PersoonInOnderzoekBeperkt
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummer
import nl.info.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import nl.info.client.brp.model.generated.RniDeelnemer
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.Waardetabel
import nl.info.client.brp.model.generated.ZoekMetGeslachtsnaamEnGeboortedatumResponse

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

fun createZoekMetGeslachtsnaamEnGeboortedatumResponse(
    persons: List<PersoonBeperkt> = listOf(createPersoonBeperkt())
) = ZoekMetGeslachtsnaamEnGeboortedatumResponse().apply {
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
    code: String = "fakeCode",
    omschrijving: String = "fakeOmschrijving"
) = Waardetabel().apply {
    this.code = code
    this.omschrijving = omschrijving
}
