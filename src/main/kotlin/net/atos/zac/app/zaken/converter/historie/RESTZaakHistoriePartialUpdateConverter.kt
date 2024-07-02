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
    fun convertPartialUpdate(auditTrail: AuditTrail, old: HashMap<*, *>, new: HashMap<*, *>) =
        old.getDiff(new).map { convertLine(auditTrail, it) }

    private fun convertLine(auditTrail: AuditTrail, change: Map.Entry<Any, Pair<*, Any?>>): RESTHistorieRegel =
        RESTHistorieRegel(
            change.key.toString(),
            convertValue(change.key, change.value.first),
            convertValue(change.key, change.value.second)
        ).apply {
            datumTijd = auditTrail.aanmaakdatum.toZonedDateTime()
            door = auditTrail.gebruikersWeergave
            toelichting = auditTrail.toelichting
        }

    private fun convertValue(resource: Any?, item: Any?): String? =
        when {
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
