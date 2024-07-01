package net.atos.zac.app.zaken.converter.historie

import jakarta.inject.Inject
import net.atos.client.vrl.VrlClientService
import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.generated.AuditTrail
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.net.URI

private const val COMMUNICATIEKANAAL = "communicatiekanaal"
private const val ZAAKGEOMETRIE = "zaakgeometrie"

class RESTZaakHistoriePartialUpdateConverter @Inject constructor(
    private val vrlClientService: VrlClientService
) {

    fun handlePartialUpdate(auditTrailRegel: AuditTrail, old: HashMap<String, *>, new: HashMap<String, *>) =
        old.getDiff(new)
            .map {
                val regel = RESTHistorieRegel(
                    it.key,
                    convertWaarde(it.key, it.value.first),
                    convertWaarde(it.key, it.value.second)
                )
                regel.datumTijd = auditTrailRegel.aanmaakdatum.toZonedDateTime()
                regel.door = auditTrailRegel.gebruikersWeergave
                regel.toelichting = auditTrailRegel.toelichting
                regel
            }

    private fun convertWaarde(resource: String, item: Any?): String? = when {
        resource == ZAAKGEOMETRIE && item is HashMap<*, *> -> item.getTypedValue(Geometry::class.java)?.toString()
        resource == COMMUNICATIEKANAAL && item is String ->
            item
                .let(URI::create)
                .let(URIUtil::parseUUIDFromResourceURI)
                .let(vrlClientService::findCommunicatiekanaal)
                .map(CommunicatieKanaal::getNaam)
                .orElse(null)
        else -> item?.toString()
    }
}
