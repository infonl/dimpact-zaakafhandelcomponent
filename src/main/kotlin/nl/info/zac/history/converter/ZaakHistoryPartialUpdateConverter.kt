/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter

import jakarta.inject.Inject
import jakarta.ws.rs.ProcessingException
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.shared.model.audit.ZrcAuditTrailRegel
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import nl.info.zac.history.model.HistoryAction
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.util.asMapWithKeyOfString
import nl.info.zac.util.diff
import nl.info.zac.util.getTypedValue
import nl.info.zac.util.time.format
import java.net.URI
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Logger

private const val RESOURCE_GERELATEERDE_ZAKEN = "gerelateerdeZaken"
private const val RESOURCE_COMMUNICATION_CHANNEL = "communicatiekanaal"
private const val RESOURCE_EINDDATUM = "einddatum"
private const val RESOURCE_EINDDATUM_GEPLAND = "einddatumGepland"
private const val RESOURCE_HOOFDZAAK = "hoofdzaak"
private const val RESOURCE_STARTDATUM = "startdatum"
private const val RESOURCE_UITERLIJKE_EINDDATUM_AFDOENING = "uiterlijkeEinddatumAfdoening"
private const val RESOURCE_EXTENSION = "verlenging"
private const val RESOURCE_ZAAKGEOMETRIE = "zaakgeometrie"

class ZaakHistoryPartialUpdateConverter @Inject constructor(
    private val zrcClientService: ZrcClientService
) {
    companion object {
        private val LOG = Logger.getLogger(ZaakHistoryPartialUpdateConverter::class.java.name)
    }

    fun convertPartialUpdate(
        auditTrailLine: ZrcAuditTrailRegel,
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
        attributeLabel = change.key,
        oldValue = change.value.first?.let { convertValue(change.key, it) },
        newValue = change.value.second?.let { convertValue(change.key, it) },
    ).apply {
        zonedDateTime = aanmaakdatum
        by = gebruikersWeergave
        this.explanation = toelichting
        this.action = actie
    }

    @Suppress("CyclomaticComplexMethod")
    private fun convertValue(resource: String, item: Any): String? =
        when (resource) {
            RESOURCE_GERELATEERDE_ZAKEN if item is List<*> -> item.asSequence()
                .filterIsInstance<Map<*, *>>()
                .mapNotNull { it["url"] as? String }
                .mapNotNull(::toUriOrNull)
                .distinct()
                .map { uri -> readZaakIdentificatieOrNull(uri) ?: uri.toString() }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ")
            RESOURCE_COMMUNICATION_CHANNEL if item is String -> item
            RESOURCE_EINDDATUM -> format(item as? String)
            RESOURCE_EINDDATUM_GEPLAND -> format(item as? String)
            RESOURCE_HOOFDZAAK if item is String ->
                item.let(URI::create)
                    .let(zrcClientService::readZaak).identificatie
            RESOURCE_STARTDATUM -> format(item as? String)
            RESOURCE_UITERLIJKE_EINDDATUM_AFDOENING -> format(item as? String)
            RESOURCE_ZAAKGEOMETRIE if item is Map<*, *> ->
                item.asMapWithKeyOfString().getTypedValue(GeoJSONGeometry::class.java)?.toHistoryLineString()
            RESOURCE_EXTENSION -> null
            else -> item.toString()
        }

    private fun toUriOrNull(uri: String): URI? =
        try {
            URI.create(uri)
        } catch (illegalArgumentException: IllegalArgumentException) {
            LOG.log(Level.FINE, "Ignoring invalid gerelateerde zaak URL '$uri'", illegalArgumentException)
            null
        }

    private fun readZaakIdentificatieOrNull(zaakUri: URI): String? =
        try {
            zrcClientService.readZaak(zaakUri).identificatie
        } catch (zgwRuntimeException: ZgwRuntimeException) {
            logUnreadableZaak(zaakUri, zgwRuntimeException)
        } catch (zgwErrorException: ZgwErrorException) {
            logUnreadableZaak(zaakUri, zgwErrorException)
        } catch (processingException: ProcessingException) {
            logUnreadableZaak(zaakUri, processingException)
        }

    private fun logUnreadableZaak(zaakUri: URI, exception: RuntimeException): String? {
        LOG.log(Level.FINE, "Cannot read gerelateerde zaak '$zaakUri'; showing its URL instead", exception)
        return null
    }
}

fun GeoJSONGeometry.toHistoryLineString() = when {
    this.type == GeometryTypeEnum.POINT -> "POINT(${this.coordinates[0]} ${this.coordinates[1]})"
    else -> throw IllegalArgumentException(
        "Geometry type '${this.type}' is currently not supported."
    )
}
