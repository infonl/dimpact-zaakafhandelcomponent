package net.atos.zac.app.klant.model.personen

import net.atos.client.brp.model.generated.OpschortingBijhouding
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.PersoonBeperkt
import net.atos.client.brp.model.generated.PersoonInOnderzoek
import net.atos.client.brp.model.generated.PersoonInOnderzoekBeperkt
import net.atos.client.brp.model.generated.RniDeelnemer

fun createPersoon(
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoek? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    indicationCuratoriesRegister: Boolean? = false,
    rniDeelnemerList: List<RniDeelnemer>? = null,
) =
    Persoon().apply {
        burgerservicenummer = "burgerservicenummer"
        geheimhoudingPersoonsgegevens = confidentialPersonalData
        inOnderzoek = personInResearch
        opschortingBijhouding = suspensionMaintenance
        indicatieCurateleRegister = indicationCuratoriesRegister
        rni = rniDeelnemerList
    }

fun createPersoonBeperkt(
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoekBeperkt? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    rniDeelnemerList: List<RniDeelnemer>? = null,
) = PersoonBeperkt().apply {
    burgerservicenummer = "burgerservicenummer"
    geheimhoudingPersoonsgegevens = confidentialPersonalData
    inOnderzoek = personInResearch
    opschortingBijhouding = suspensionMaintenance
    rni = rniDeelnemerList
}
