package net.atos.zac.app.klant.model.personen

import net.atos.client.brp.model.generated.AbstractDatum
import net.atos.client.brp.model.generated.OpschortingBijhouding
import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.PersoonInOnderzoek
import net.atos.client.brp.model.generated.RniDeelnemer
import java.time.ZonedDateTime

fun createPersoon(
    confidentialPersonalData: Boolean = false,
    personInResearch: PersoonInOnderzoek? = null,
    suspensionMaintenance: OpschortingBijhouding? = null,
    indicationCuratoriesRegister: Boolean? = false,
    rniDeelnemerList: List<RniDeelnemer>? = null,
) =
    Persoon().apply {
        burgerservicenummer = "burgerservicenummer"
        datumEersteInschrijvingGBA = AbstractDatum().apply {
            type = "type"
            langFormaat = ZonedDateTime.now().toString()
        }
        geheimhoudingPersoonsgegevens = confidentialPersonalData
        inOnderzoek = personInResearch
        opschortingBijhouding = suspensionMaintenance
        indicatieCurateleRegister = indicationCuratoriesRegister
        rni = rniDeelnemerList
    }
