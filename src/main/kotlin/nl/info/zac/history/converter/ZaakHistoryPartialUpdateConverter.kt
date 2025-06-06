/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter

import jakarta.inject.Inject
import net.atos.zac.util.time.LocalDateUtil
import nl.info.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.history.model.HistoryAction
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.util.asMapWithKeyOfString
import nl.info.zac.util.diff
import nl.info.zac.util.getTypedValue
import nl.info.zac.util.stringProperty
import java.net.URI
import java.time.ZonedDateTime

private const val KEY_URL = "url"
private const val RESOURCE_COMMUNICATION_CHANNEL = "communicatiekanaal"
private const val RESOURCE_EINDDATUM = "einddatum"
private const val RESOURCE_EINDDATUM_GEPLAND = "einddatumGepland"
private const val RESOURCE_HOOFDZAAK = "hoofdzaak"
private const val RESOURCE_RELEVANTE_ANDERE_ZAKEN = "relevanteAndereZaken"
private const val RESOURCE_STARTDATUM = "startdatum"
private const val RESOURCE_UITERLIJKE_EINDDATUM_AFDOENING = "uiterlijkeEinddatumAfdoening"
private const val RESOURCE_EXTENSION = "verlenging"
private const val RESOURCE_ZAAKGEOMETRIE = "zaakgeometrie"

class ZaakHistoryPartialUpdateConverter @Inject constructor(
    private val zrcClientService: ZrcClientService
) {
    fun convertPartialUpdate(
        auditTrailLine: ZRCAuditTrailRegel,
        historyAction: HistoryAction?,
        oldValues: Map<String, *>,
        newValues: Map<String, *>
    ) = oldValues.diff(newValues).map {
        convertLine(
            aanmaakdatum = auditTrailLine.aanmaakdatum,
            gebruikersWeergave = auditTrailLine.gebruikersWeergave,
            toelichting = auditTrailLine.toelichting,
            actie = historyAction,
            change = it
        )
    }

    private fun convertLine(
        aanmaakdatum: ZonedDateTime,
        gebruikersWeergave: String?,
        toelichting: String?,
        actie: HistoryAction?,
        change: Map.Entry<String, Pair<*, *>>
    ) = HistoryLine(
        attribuutLabel = change.key,
        oudeWaarde = change.value.first?.let { convertValue(change.key, it) },
        nieuweWaarde = change.value.second?.let { convertValue(change.key, it) },
    ).apply {
        datumTijd = aanmaakdatum
        door = gebruikersWeergave
        this.toelichting = toelichting
        this.actie = actie
    }

    @Suppress("CyclomaticComplexMethod")
    private fun convertValue(resource: String, item: Any): String? =
        when {
            resource == RESOURCE_COMMUNICATION_CHANNEL && item is String -> item
            resource == RESOURCE_EINDDATUM -> LocalDateUtil.format(item as? String)
            resource == RESOURCE_EINDDATUM_GEPLAND -> LocalDateUtil.format(item as? String)
            resource == RESOURCE_HOOFDZAAK && item is String ->
                item
                    .let(URI::create)
                    .let(zrcClientService::readZaak).identificatie
            resource == RESOURCE_RELEVANTE_ANDERE_ZAKEN && item is List<*> ->
                item
                    .asSequence()
                    .mapNotNull { (it as? Map<*, *>)?.asMapWithKeyOfString()?.stringProperty(KEY_URL) }
                    .map(URI::create)
                    .map(zrcClientService::readZaak)
                    .joinToString { it.identificatie }
            resource == RESOURCE_STARTDATUM -> LocalDateUtil.format(item as? String)
            resource == RESOURCE_UITERLIJKE_EINDDATUM_AFDOENING -> LocalDateUtil.format(item as? String)
            resource == RESOURCE_ZAAKGEOMETRIE && item is Map<*, *> ->
                item.asMapWithKeyOfString().getTypedValue(GeoJSONGeometry::class.java)?.toHistoryLineString()
            resource == RESOURCE_EXTENSION -> null
            else -> item.toString()
        }
}

fun GeoJSONGeometry.toHistoryLineString() = when {
    this.type == GeometryTypeEnum.POINT -> "POINT(${this.coordinates[0]} ${this.coordinates[1]})"
    else -> throw IllegalArgumentException(
        "Geometry type '${this.type}' is currently not supported."
    )
}
