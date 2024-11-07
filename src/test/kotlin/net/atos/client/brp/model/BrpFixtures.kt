/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp.model

import net.atos.client.brp.model.generated.OpschortingBijhouding
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.PersoonBeperkt
import net.atos.client.brp.model.generated.PersoonInOnderzoek
import net.atos.client.brp.model.generated.PersoonInOnderzoekBeperkt
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse
import net.atos.client.brp.model.generated.RniDeelnemer

@Suppress("LongParameterList")
fun createPersoon(
    bsn: String = "123456789",
    age: Int = 18,
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoek? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    indicationCuratoriesRegister: Boolean? = false,
    rniDeelnemerList: List<RniDeelnemer>? = null,
) =
    Persoon().apply {
        burgerservicenummer = bsn
        leeftijd = age
        burgerservicenummer = "burgerservicenummer"
        geheimhoudingPersoonsgegevens = confidentialPersonalData
        inOnderzoek = personInResearch
        opschortingBijhouding = suspensionMaintenance
        indicatieCurateleRegister = indicationCuratoriesRegister
        rni = rniDeelnemerList
    }

@Suppress("LongParameterList")
fun createPersoonBeperkt(
    bsn: String = "123456789",
    age: Int = 18,
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoekBeperkt? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    rniDeelnemerList: List<RniDeelnemer>? = null,
) = PersoonBeperkt().apply {
    burgerservicenummer = bsn
    leeftijd = age
    burgerservicenummer = "burgerservicenummer"
    geheimhoudingPersoonsgegevens = confidentialPersonalData
    inOnderzoek = personInResearch
    opschortingBijhouding = suspensionMaintenance
    rni = rniDeelnemerList
}

fun createRaadpleegMetBurgerservicenummerResponse(
    persons: List<Persoon> = listOf(createPersoon())
) = RaadpleegMetBurgerservicenummerResponse().apply {
    personen = persons
}
