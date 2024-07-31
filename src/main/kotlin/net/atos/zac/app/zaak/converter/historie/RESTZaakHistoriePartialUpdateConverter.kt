package net.atos.zac.app.zaak.converter.historie

import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Geometry
import net.atos.zac.app.audit.model.RESTHistorieActie
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.net.URI

private const val COMMUNICATIEKANAAL = "communicatiekanaal"
private const val ZAAKGEOMETRIE = "zaakgeometrie"
private const val HOOFDZAAK = "hoofdzaak"
private const val RELEVANTE_ANDERE_ZAKEN = "relevanteAndereZaken"

class RESTZaakHistoriePartialUpdateConverter @Inject constructor(
    private val zrcClientService: ZrcClientService
) {
    fun convertPartialUpdate(
        auditTrail: ZRCAuditTrailRegel,
        actie: RESTHistorieActie?,
        old: Map<String, *>,
        new: Map<String, *>
    ) =
        old.diff(new).map { convertLine(auditTrail, actie, it) }

    private fun convertLine(
        auditTrail: ZRCAuditTrailRegel,
        actie: RESTHistorieActie?,
        change: Map.Entry<String, Pair<*, *>>
    ): RESTHistorieRegel =
        RESTHistorieRegel(
            change.key,
            change.value.first?.let { convertValue(change.key, it) },
            change.value.second?.let { convertValue(change.key, it) },
        ).apply {
            datumTijd = auditTrail.aanmaakdatum
            door = auditTrail.gebruikersWeergave
            toelichting = auditTrail.toelichting
            this.actie = actie
        }

    private fun convertValue(resource: String, item: Any): String? =
        when {
            resource == ZAAKGEOMETRIE && item is Map<*, *> -> item.asMapWithKeyOfString().getTypedValue(
                Geometry::class.java
            )?.toString()
            resource == COMMUNICATIEKANAAL && item is String -> item
            resource == HOOFDZAAK && item is String ->
                item
                    .let(URI::create)
                    .let(zrcClientService::readZaak).identificatie
            resource == RELEVANTE_ANDERE_ZAKEN && item is List<*> ->
                item
                    .asSequence()
                    .mapNotNull { (it as? Map<*, *>)?.asMapWithKeyOfString()?.stringProperty("url") }
                    .map(URI::create)
                    .map(zrcClientService::readZaak)
                    .joinToString { it.identificatie }
            else -> item.toString()
        }
}
