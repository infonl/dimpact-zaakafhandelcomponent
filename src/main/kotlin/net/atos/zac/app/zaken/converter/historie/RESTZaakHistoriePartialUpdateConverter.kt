package net.atos.zac.app.zaken.converter.historie

import jakarta.inject.Inject
import net.atos.client.vrl.VrlClientService
import net.atos.client.vrl.model.generated.CommunicatieKanaal
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.shared.util.URIUtil
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Geometry
import net.atos.zac.app.audit.model.RESTHistorieActie
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.net.URI

private const val COMMUNICATIEKANAAL = "communicatiekanaal"
private const val ZAAKGEOMETRIE = "zaakgeometrie"

class RESTZaakHistoriePartialUpdateConverter @Inject constructor(
    private val vrlClientService: VrlClientService,
    private val zrcClientService: ZRCClientService
) {
    fun convertPartialUpdate(
        auditTrail: ZRCAuditTrailRegel,
        actie: RESTHistorieActie?,
        old: Map<*, *>,
        new: Map<*, *>
    ) =
        old.diff(new).map { convertLine(auditTrail, actie, it) }

    private fun convertLine(
        auditTrail: ZRCAuditTrailRegel,
        actie: RESTHistorieActie?,
        change: Map.Entry<Any?, Pair<*, Any?>>
    ): RESTHistorieRegel =
        RESTHistorieRegel(
            change.key.toString(),
            convertValue(change.key, change.value.first),
            convertValue(change.key, change.value.second)
        ).apply {
            datumTijd = auditTrail.aanmaakdatum
            door = auditTrail.gebruikersWeergave
            toelichting = auditTrail.toelichting
            this.actie = actie
        }

    private fun convertValue(resource: Any?, item: Any?): String? =
        when {
            resource == ZAAKGEOMETRIE && item is Map<*, *> -> item.getTypedValue(Geometry::class.java)?.toString()
            resource == COMMUNICATIEKANAAL && item is String ->
                item
                    .let(URI::create)
                    .let(URIUtil::parseUUIDFromResourceURI)
                    .let(vrlClientService::findCommunicatiekanaal)
                    .map(CommunicatieKanaal::getNaam)
                    .orElse(null)
            resource == "hoofdzaak" && item is String ->
                item
                    .let(URI::create)
                    .let(zrcClientService::readZaak).identificatie
            resource == "relevanteAndereZaken" && item is List<*> ->
                item
                    .asSequence()
                    .mapNotNull { (it as? Map<*, *>)?.stringProperty("url") }
                    .map(URI::create)
                    .map(zrcClientService::readZaak)
                    .joinToString { it.identificatie }
            else -> item?.toString()
        }
}
