package net.atos.zac.app.audit.converter.documenten

import net.atos.client.zgw.drc.model.generated.Gebruiksrechten
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.documenten.GebuiksrechtenWijziging
import net.atos.zac.app.audit.converter.AbstractAuditWijzigingConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.util.stream.Stream

class AuditGebruiksrechtenWijzigingConverter : AbstractAuditWijzigingConverter<GebuiksrechtenWijziging>() {
    override fun supports(objectType: ObjectType): Boolean {
        return ObjectType.GEBRUIKSRECHTEN == objectType
    }

    override fun doConvert(wijziging: GebuiksrechtenWijziging): Stream<RESTHistorieRegel?> {
        return Stream.of(
            RESTHistorieRegel(
                "indicatieGebruiksrecht",
                toWaarde(wijziging.oud),
                toWaarde(wijziging.nieuw)
            )
        )
    }

    private fun toWaarde(gebruiksrechten: Gebruiksrechten?): String? {
        return gebruiksrechten?.omschrijvingVoorwaarden
    }
}
